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
import com.google.android.ground.ui.common.AbstractMapFragmentWithControls
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.MapFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DropPinTaskMapFragment @Inject constructor() : AbstractMapFragmentWithControls() {

  private lateinit var mapViewModel: BaseMapViewModel
  private val viewModel: DropPinTaskViewModel by lazy {
    // Access to this viewModel is lazy for testing. This is because the NavHostController could
    // not be initialized before the Fragment under test is created, leading to
    // hiltNavGraphViewModels() to fail when called on launch.
    dataCollectionViewModel.getTaskViewModel(taskId) as DropPinTaskViewModel
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mapViewModel = getViewModel(BaseMapViewModel::class.java)
  }

  override fun getMapViewModel(): BaseMapViewModel = mapViewModel

  override fun onMapReady(map: MapFragment) {
    super.onMapReady(map)
    viewModel.features.observe(this) { map.setFeatures(it) }
  }

  override fun onMapCameraMoved(position: CameraPosition) {
    super.onMapCameraMoved(position)
    viewModel.updateCameraPosition(position)
  }
}
