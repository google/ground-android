/*
 * Copyright 2021 Google LLC
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
package com.google.android.ground.ui.home.mapcontainer

import android.content.Context
import com.google.android.ground.R
import com.google.android.ground.databinding.PolygonDrawingControlsBinding
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.rx.RxAutoDispose
import com.google.android.ground.ui.common.AbstractView
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.MapFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings

@WithFragmentBindings
@AndroidEntryPoint
class PolygonDrawingView(context: Context, mapFragment: MapFragment) : AbstractView(context) {
  init {
    val viewModel = getViewModel(PolygonDrawingViewModel::class.java)
    val binding = inflate(R.layout.polygon_drawing_controls) as PolygonDrawingControlsBinding
    binding.viewModel = viewModel

    mapFragment.cameraMovedEvents
      .onBackpressureLatest()
      .map(CameraPosition::target)
      .doOnNext { viewModel.onCameraMoved(it) }
      .doOnNext { mapCenter ->
        viewModel.firstVertex
          .map { firstVertex: Point -> mapFragment.getDistanceInPixels(firstVertex, mapCenter) }
          .ifPresent { dist: Double -> viewModel.updateLastVertex(mapCenter, dist) }
      }
      .`as`(RxAutoDispose.disposeOnDestroy(activity))
      .subscribe()

    // Using this approach as data binding approach did not work with view.
    viewModel.isPolygonCompleted.observe(activity) { isComplete ->
      binding.completePolygonButton.visibility = if (isComplete) VISIBLE else GONE
      binding.addPolygonButton.visibility = if (isComplete) GONE else VISIBLE
    }
  }
}
