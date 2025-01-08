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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.ground.ui.common.MapConfig
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskMapFragment
import com.google.android.ground.ui.map.MapFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CaptureLocationTaskMapFragment @Inject constructor() :
  AbstractTaskMapFragment<CaptureLocationTaskViewModel>() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    val root = super.onCreateView(inflater, container, savedInstanceState)
    viewLifecycleOwner.lifecycleScope.launch {
      getMapViewModel().getLocationUpdates().collect { taskViewModel.updateLocation(it) }
    }
    return root
  }

  override fun getMapConfig(): MapConfig = super.getMapConfig().copy(allowGestures = false)

  override fun onMapReady(map: MapFragment) {
    super.onMapReady(map)
    viewLifecycleOwner.lifecycleScope.launch {
      taskViewModel.initLocationUpdates(getMapViewModel())
    }
  }
}
