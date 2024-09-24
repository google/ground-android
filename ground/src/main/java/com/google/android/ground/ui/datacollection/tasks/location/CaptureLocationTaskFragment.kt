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
package com.google.android.ground.ui.datacollection.tasks.location

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.google.android.ground.R
import com.google.android.ground.model.submission.isNotNullOrEmpty
import com.google.android.ground.model.submission.isNullOrEmpty
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewFactory
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import com.google.android.ground.ui.map.MapFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CaptureLocationTaskFragment : AbstractTaskFragment<CaptureLocationTaskViewModel>() {

  @Inject lateinit var map: MapFragment

  override fun onCreateTaskView(inflater: LayoutInflater): TaskView =
    TaskViewFactory.createWithCombinedHeader(inflater, R.drawable.outline_pin_drop)

  override fun onCreateTaskBody(inflater: LayoutInflater): View {
    // NOTE(#2493): Multiplying by a random prime to allow for some mathematical uniqueness.
    // Otherwise, the sequentially generated ID might conflict with an ID produced by Google Maps.
    val rowLayout = LinearLayout(requireContext()).apply { id = View.generateViewId() * 11149 }
    parentFragmentManager
      .beginTransaction()
      .add(
        rowLayout.id,
        CaptureLocationTaskMapFragment.newInstance(viewModel, map),
        CaptureLocationTaskMapFragment::class.java.simpleName,
      )
      .commit()
    return rowLayout
  }

  override fun onTaskResume() {
    // Ensure that the location lock is enabled, if it hasn't been.
    viewModel.enableLocationLock()
  }

  override fun onCreateActionButtons() {
    addSkipButton()
    addUndoButton()
    addButton(ButtonAction.CAPTURE_LOCATION)
      .setOnClickListener { viewModel.updateResponse() }
      .setOnValueChanged { button, value -> button.showIfTrue(value.isNullOrEmpty()) }
    addButton(ButtonAction.NEXT)
      .setOnClickListener { handleNext() }
      .setOnValueChanged { button, value ->
        button.showIfTrue(value.isNotNullOrEmpty())
        button.toggleDone(checkLastPositionWithTaskData(value))
      }
      .hide()
  }
}
