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
import androidx.lifecycle.lifecycleScope
import com.google.android.ground.R
import com.google.android.ground.model.geometry.GeometryValidator.isClosed
import com.google.android.ground.ui.MarkerIconFactory
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewWithoutHeader
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.MapFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

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
    addButton(ButtonAction.COMPLETE).setOnClickListener { viewModel.onCompletePolygonButtonClick() }
    addUndoButton()
    addSkipButton()
  }

  override fun onTaskViewAttached() {
    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.featureValue.collect { onFeatureUpdated(it) }
    }
  }

  private fun onFeatureUpdated(feature: Feature?) {
    val vertexCount = feature?.geometry?.size ?: 0
    val isClosedGeometry = feature?.geometry.isClosed()
    val isMarkedComplete = viewModel.isMarkedComplete()

    getButton(ButtonAction.ADD_POINT).showIfTrue(!isClosedGeometry)
    getButton(ButtonAction.COMPLETE).showIfTrue(isClosedGeometry && !isMarkedComplete)
    getButton(ButtonAction.CONTINUE).showIfTrue(isMarkedComplete)
    getButton(ButtonAction.UNDO).showIfTrue(vertexCount > 1)
  }
}
