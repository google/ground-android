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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.android.ground.R
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.ui.MarkerIconFactory
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewWithoutHeader
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import com.google.android.ground.ui.map.MapFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PolygonDrawingTaskFragment : AbstractTaskFragment<PolygonDrawingViewModel>() {

  @Inject lateinit var markerIconFactory: MarkerIconFactory
  @Inject lateinit var mapFragment: MapFragment

  override fun onCreateTaskView(inflater: LayoutInflater, container: ViewGroup?): TaskView =
    TaskViewWithoutHeader.create(inflater, R.drawable.outline_draw, R.string.draw_an_area)

  override fun onCreateTaskBody(inflater: LayoutInflater): View {
    val rowLayout = LinearLayout(requireContext()).apply { id = View.generateViewId() }
    parentFragmentManager
      .beginTransaction()
      .add(
        rowLayout.id,
        PolygonDrawingMapFragment.newInstance(viewModel, mapFragment),
        "Draw a polygon fragment"
      )
      .commit()
    return rowLayout
  }

  override fun onCreateActionButtons() {
    addContinueButton()
    addButton(ButtonAction.ADD_POINT).setOnClickListener { viewModel.addLastVertex() }
    addButton(ButtonAction.COMPLETE).setOnClickListener {
      viewModel.onCompletePolygonButtonClick()
      getButton(ButtonAction.CONTINUE).show()
      getButton(ButtonAction.COMPLETE).hide()
    }
    addUndoButton()
    addSkipButton()
  }

  override fun onTaskViewAttached() {
    viewModel.polygonLiveData.observe(viewLifecycleOwner) { onPolygonUpdated(it) }
  }

  private fun onPolygonUpdated(polygon: Polygon) {
    val addPointButton = getButton(ButtonAction.ADD_POINT)
    val completeButton = getButton(ButtonAction.COMPLETE)
    val continueButton = getButton(ButtonAction.CONTINUE)
    val undoButton = getButton(ButtonAction.UNDO)

    if (polygon.isEmpty) {
      addPointButton.show()
      completeButton.hide()
      continueButton.hide()
      undoButton.hide()
    } else if (polygon.isComplete) {
      addPointButton.hide()
      completeButton.show()
      continueButton.hide()
      undoButton.show()
    } else {
      continueButton.hide()
      addPointButton.show()
      completeButton.hide()
      if (polygon.size > 1) undoButton.show() else undoButton.hide()
    }
  }
}
