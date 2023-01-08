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
import com.google.android.ground.databinding.MapMoveLocationOfInterestLayoutBinding
import com.google.android.ground.rx.RxAutoDispose
import com.google.android.ground.ui.common.AbstractView
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.MapFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings

@WithFragmentBindings
@AndroidEntryPoint
class LocationOfInterestRepositionView(context: Context, mapFragment: MapFragment) :
  AbstractView(context) {
  init {
    val viewModel = getViewModel(LocationOfInterestRepositionViewModel::class.java)
    val binding =
      inflate(R.layout.map_move_location_of_interest_layout)
        as MapMoveLocationOfInterestLayoutBinding
    binding.viewModel = viewModel

    mapFragment.cameraMovedEvents
      .map(CameraPosition::target)
      .onBackpressureLatest()
      .`as`(RxAutoDispose.disposeOnDestroy(activity))
      .subscribe { viewModel.onCameraMoved(it) }
  }
}
