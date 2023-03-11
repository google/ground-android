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
package com.google.android.ground.ui.datacollection.tasks.polygon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.android.ground.R
import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.ui.MarkerIconFactory
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.components.TaskViewWithoutHeader
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import com.google.android.ground.ui.datacollection.tasks.TaskFragment
import com.google.android.ground.ui.map.MapFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.properties.Delegates

@AndroidEntryPoint
class PolygonDrawingTaskFragment : AbstractTaskFragment<PolygonDrawingViewModel>() {
  override lateinit var viewModel: PolygonDrawingViewModel
  override var position by Delegates.notNull<Int>()

  @Inject lateinit var markerIconFactory: MarkerIconFactory
  @Inject lateinit var mapFragment: MapFragment

  private lateinit var mapViewModel: BaseMapViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) {
      position = savedInstanceState.getInt(TaskFragment.POSITION)
    }
    mapViewModel = getViewModel(BaseMapViewModel::class.java)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putInt(TaskFragment.POSITION, position)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    viewModel = dataCollectionViewModel.getTaskViewModel(position) as PolygonDrawingViewModel

    // Base template with just a footer
    taskView = TaskViewWithoutHeader.create(container, inflater, this, viewModel)

    // Task view
    val rowLayout = LinearLayout(requireContext()).apply { id = View.generateViewId() }
    parentFragmentManager
      .beginTransaction()
      .add(
        rowLayout.id,
        PolygonDrawingMapFragment.newInstance(viewModel, mapViewModel, mapFragment),
        "Draw a polygon fragment"
      )
      .commit()
    taskView.addTaskView(rowLayout)

    return taskView.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    viewModel.startDrawingFlow()
    viewModel.isPolygonCompleted.observe(viewLifecycleOwner) { onPolygonUpdated(it) }
  }

  override fun onCreateActionButtons() {
    createButton(R.string.add_point, R.drawable.ic_add, ButtonAction.ADD_PIN) {
      viewModel.selectCurrentVertex()
    }
    createButton(R.string.complete, null, ButtonAction.COMPLETE) {
      viewModel.onCompletePolygonButtonClick()
    }
    addContinueButton()
    addSkipButton()
    createButton(null, R.drawable.ic_undo_black, ButtonAction.UNDO) { viewModel.removeLastVertex() }
  }

  private fun onPolygonUpdated(isPolygonComplete: Boolean) {
    getButton(ButtonAction.ADD_PIN).apply {
      isEnabled = true
      visibility = if (isPolygonComplete) View.GONE else View.VISIBLE
    }
    getButton(ButtonAction.COMPLETE).apply {
      isEnabled = true
      visibility = if (isPolygonComplete) View.VISIBLE else View.GONE
    }
  }

  override fun refreshState(taskData: TaskData?) {
    val isTaskEmpty = taskData?.isEmpty() ?: true
    getButton(ButtonAction.CONTINUE).apply {
      isEnabled = true
      visibility = if (isTaskEmpty) View.GONE else View.VISIBLE
    }
    getButton(ButtonAction.UNDO).apply { visibility = if (isTaskEmpty) View.GONE else View.VISIBLE }
  }
}
