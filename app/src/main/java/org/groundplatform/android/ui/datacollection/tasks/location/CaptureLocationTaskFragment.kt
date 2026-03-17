/*
 * Copyright 2023 Google LLC
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
package org.groundplatform.android.ui.datacollection.tasks.location

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.ui.components.ConfirmationDialog
import org.groundplatform.android.ui.datacollection.components.Header
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskMapFragment.Companion.TASK_ID_FRAGMENT_ARG_KEY
import org.groundplatform.android.ui.datacollection.tasks.LocationLockEnabledState

@AndroidEntryPoint
class CaptureLocationTaskFragment @Inject constructor() :
  AbstractTaskFragment<CaptureLocationTaskViewModel>() {
  @Inject
  lateinit var captureLocationTaskMapFragmentProvider: Provider<CaptureLocationTaskMapFragment>

  override val taskHeader: Header by lazy {
    Header(viewModel.task.label, R.drawable.outline_pin_drop)
  }

  @Composable
  override fun TaskBody() {
    var showPermissionDeniedDialog by viewModel.showPermissionDeniedDialog

    AndroidView(
      factory = { context ->
        // NOTE(#2493): Multiplying by a random prime to allow for some mathematical uniqueness.
        // Otherwise, the sequentially generated ID might conflict with an ID produced by Google
        // Maps.
        LinearLayout(context).apply {
          id = View.generateViewId() * 11149
          val fragment = captureLocationTaskMapFragmentProvider.get()
          fragment.arguments = bundleOf(Pair(TASK_ID_FRAGMENT_ARG_KEY, taskId))
          childFragmentManager
            .beginTransaction()
            .add(id, fragment, CaptureLocationTaskMapFragment::class.java.simpleName)
            .commit()
        }
      }
    )

    if (showPermissionDeniedDialog) {
      ConfirmationDialog(
        title = R.string.allow_location_title,
        description = R.string.allow_location_description,
        confirmButtonText = R.string.allow_location_confirmation,
        onConfirmClicked = {
          showPermissionDeniedDialog = false

          // Open the app settings
          val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
          intent.data = Uri.fromParts("package", context?.packageName, null)
          context?.startActivity(intent)
        },
      )
    }
  }

  override fun onTaskResume() {
    // Ensure that the location lock is enabled, if it hasn't been.
    if (isVisible) {
      viewModel.enableLocationLock()
      lifecycleScope.launch {
        viewModel.enableLocationLockFlow.collect {
          if (it == LocationLockEnabledState.NEEDS_ENABLE) {
            viewModel.showPermissionDeniedDialog.value = true
          }
        }
      }
    }
  }

  override fun shouldShowHeader() = true

  @Composable
  override fun HeaderCard() {
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
