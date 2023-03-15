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
package com.google.android.ground.ui.datacollection.tasks.point

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.ground.R
import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.ui.MarkerIconFactory
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewWithoutHeader
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import com.google.android.ground.ui.map.MapFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DropAPinTaskFragment : AbstractTaskFragment<DropAPinTaskViewModel>() {

  @Inject lateinit var markerIconFactory: MarkerIconFactory
  @Inject lateinit var mapFragment: MapFragment

  private lateinit var mapViewModel: BaseMapViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mapViewModel = getViewModel(BaseMapViewModel::class.java)
  }

  override fun onCreateTaskView(inflater: LayoutInflater, container: ViewGroup?): TaskView {
    return TaskViewWithoutHeader.create(container, inflater)
  }

  override fun onCreateTaskBody(inflater: LayoutInflater): View {
    val rowLayout = LinearLayout(requireContext()).apply { id = View.generateViewId() }
    parentFragmentManager
      .beginTransaction()
      .add(
        rowLayout.id,
        DropAPinMapFragment.newInstance(viewModel, mapViewModel, mapFragment),
        "Drop a pin fragment"
      )
      .commit()
    return rowLayout
  }

  override fun onCreateActionButtons() {
    createButton(R.string.drop_pin, null, ButtonAction.DROP_PIN) {
      Toast.makeText(requireContext(), "TODO: Add a marker at the center", Toast.LENGTH_SHORT)
        .show()
    }
    addContinueButton()
    addSkipButton()
    addUndoButton()
  }

  override fun refreshState(taskData: TaskData?) {
    val isTaskEmpty = taskData?.isEmpty() ?: true
    getButton(ButtonAction.CONTINUE).apply {
      isEnabled = true
      visibility = if (isTaskEmpty) View.GONE else View.VISIBLE
    }
    getButton(ButtonAction.UNDO).apply { visibility = if (isTaskEmpty) View.GONE else View.VISIBLE }
    getButton(ButtonAction.DROP_PIN).apply {
      isEnabled = true
      visibility = if (isTaskEmpty) View.VISIBLE else View.GONE
    }
  }
}
