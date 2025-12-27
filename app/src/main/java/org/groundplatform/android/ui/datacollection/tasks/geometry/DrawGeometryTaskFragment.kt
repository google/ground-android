/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.groundplatform.android.ui.datacollection.tasks.geometry

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.model.submission.isNullOrEmpty
import org.groundplatform.android.ui.components.ConfirmationDialog
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.InstructionsDialog
import org.groundplatform.android.ui.datacollection.components.TaskView
import org.groundplatform.android.ui.datacollection.components.TaskViewFactory
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskMapFragment.Companion.TASK_ID_FRAGMENT_ARG_KEY
import org.groundplatform.android.ui.datacollection.tasks.LocationLockEnabledState
import org.groundplatform.android.ui.datacollection.tasks.location.LocationAccuracyCard
import org.groundplatform.android.util.renderComposableDialog

/**
 * A fragment for displaying and handling the "draw geometry" task.
 *
 * This task allows the user to define a geometry (e.g. a point, polygon, etc.) on the map.
 * Depending on the configuration, it may require the user's location to be locked and accurate.
 */
@AndroidEntryPoint
class DrawGeometryTaskFragment @Inject constructor() :
  AbstractTaskFragment<DrawGeometryTaskViewModel>() {
  @Inject lateinit var drawGeometryTaskMapFragmentProvider: Provider<DrawGeometryTaskMapFragment>

  override fun onCreateTaskView(inflater: LayoutInflater): TaskView =
    TaskViewFactory.createWithCombinedHeader(
      inflater,
      if (viewModel.isDrawAreaMode()) R.drawable.outline_draw else R.drawable.outline_pin_drop,
    )

  override fun onCreateTaskBody(inflater: LayoutInflater): View {
    // NOTE(#2493): Multiplying by a random prime to allow for some mathematical uniqueness.
    val rowLayout = LinearLayout(requireContext()).apply { id = View.generateViewId() * 11149 }
    val fragment = drawGeometryTaskMapFragmentProvider.get()
    val args = Bundle()
    args.putString(TASK_ID_FRAGMENT_ARG_KEY, taskId)
    fragment.arguments = args
    childFragmentManager
      .beginTransaction()
      .add(rowLayout.id, fragment, DrawGeometryTaskMapFragment::class.java.simpleName)
      .commit()
    return rowLayout
  }

  override fun onTaskResume() {
    // Ensure that the location lock is enabled, if it hasn't been.
    if (isVisible) {
      if (viewModel.isLocationLockRequired()) {
        viewModel.enableLocationLock()
        lifecycleScope.launch {
          viewModel.enableLocationLockFlow.collect {
            if (it == LocationLockEnabledState.NEEDS_ENABLE) {
              showLocationPermissionDialog()
            }
          }
        }
      } else if (viewModel.shouldShowInstructionsDialog()) {
        showInstructionsDialog()
      }
    }

    viewModel.polygonArea.observe(viewLifecycleOwner) { area ->
      android.widget.Toast.makeText(
          requireContext(),
          getString(R.string.area_message, area),
          android.widget.Toast.LENGTH_LONG,
        )
        .show()
    }
  }

  override fun onCreateActionButtons() {
    if (viewModel.isDrawAreaMode()) {
      val addPointButton =
        addButton(ButtonAction.ADD_POINT).setOnClickListener {
          viewModel.addLastVertex()
          val intersected = viewModel.checkVertexIntersection()
          if (!intersected) viewModel.triggerVibration()
        }
      val completeButton =
        addButton(ButtonAction.COMPLETE)
          .setOnClickListener { viewModel.completePolygon() }
          .setOnValueChanged { button, _ ->
            button.enableIfTrue(viewModel.validatePolygonCompletion())
          }
      val undoButton =
        addButton(ButtonAction.UNDO)
          .setOnClickListener { viewModel.removeLastVertex() }
          .setOnValueChanged { button, value -> button.enableIfTrue(!value.isNullOrEmpty()) }
      val redoButton =
        addButton(ButtonAction.REDO)
          .setOnClickListener { viewModel.redoLastVertex() }
          .setOnValueChanged { button, value ->
            button.enableIfTrue(viewModel.redoVertexStack.isNotEmpty() && !value.isNullOrEmpty())
          }
      val nextButton = addNextButton(hideIfEmpty = true)

      viewLifecycleOwner.lifecycleScope.launch {
        viewModel.isMarkedComplete.collect { markedComplete ->
          addPointButton.showIfTrue(!markedComplete)
          completeButton.showIfTrue(!markedComplete)
          undoButton.showIfTrue(!markedComplete)
          redoButton.showIfTrue(!markedComplete)
          nextButton.showIfTrue(markedComplete)
        }
      }
    } else {
      addSkipButton()
      addUndoButton()

      if (viewModel.isLocationLockRequired()) {
        addButton(ButtonAction.CAPTURE_LOCATION)
          .setOnClickListener { viewModel.onCaptureLocation() }
          .setOnValueChanged { button, value -> button.showIfTrue(value.isNullOrEmpty()) }
          .apply {
            viewLifecycleOwner.lifecycleScope.launch {
              viewModel.isCaptureEnabled.collect { isEnabled -> enableIfTrue(isEnabled) }
            }
          }
      } else {
        addButton(ButtonAction.DROP_PIN)
          .setOnClickListener { viewModel.onDropPin() }
          .setOnValueChanged { button, value -> button.showIfTrue(value.isNullOrEmpty()) }
      }

      addNextButton(hideIfEmpty = true)
    }
  }

  @Composable
  override fun HeaderCard() {
    if (viewModel.isLocationLockRequired()) {
      val location by viewModel.lastLocation.collectAsState()
      var showAccuracyCard by remember { mutableStateOf(false) }

      LaunchedEffect(location) {
        showAccuracyCard = location != null && !viewModel.isCaptureEnabled.first()
      }

      if (showAccuracyCard) {
        LocationAccuracyCard(
          onDismiss = { showAccuracyCard = false },
          modifier = Modifier.padding(bottom = 12.dp),
        )
      }
    }
  }

  private fun showLocationPermissionDialog() {
    renderComposableDialog {
      ConfirmationDialog(
        title = R.string.allow_location_title,
        description = R.string.allow_location_description,
        confirmButtonText = R.string.allow_location_confirmation,
        onConfirmClicked = {
          // Open the app settings
          val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
          intent.data = Uri.fromParts("package", context?.packageName, null)
          context?.startActivity(intent)
        },
      )
    }
  }

  private fun showInstructionsDialog() {
    viewModel.instructionsDialogShown = true
    renderComposableDialog {
      InstructionsDialog(
        iconId = if (viewModel.isDrawAreaMode()) R.drawable.touch_app_24 else R.drawable.swipe_24,
        stringId =
          if (viewModel.isDrawAreaMode()) R.string.draw_area_task_instruction
          else R.string.drop_a_pin_tooltip_text,
      )
    }
  }
}
