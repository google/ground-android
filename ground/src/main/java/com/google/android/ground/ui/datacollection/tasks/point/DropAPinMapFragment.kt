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
import com.google.android.ground.R
import com.google.android.ground.ui.common.AbstractMapFragmentWithControls
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.datacollection.components.TaskHeaderPopupView
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.MapFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint(AbstractMapFragmentWithControls::class)
class DropAPinMapFragment(private val viewModel: DropAPinTaskViewModel) :
  Hilt_DropAPinMapFragment() {

  private lateinit var mapViewModel: BaseMapViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mapViewModel = getViewModel(BaseMapViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val root = super.onCreateView(inflater, container, savedInstanceState)
    binding.hintIcon.setOnClickListener {
      TaskHeaderPopupView(requireContext())
        .show(binding.hintIcon, getString(R.string.drop_a_pin_tooltip_text))
    }
    binding.hintIcon.visibility = View.VISIBLE
    return root
  }

  override fun getMapViewModel(): BaseMapViewModel = mapViewModel

  override fun onMapReady(map: MapFragment) {
    viewModel.features.observe(this) { map.renderFeatures(it) }
  }

  override fun onMapCameraMoved(position: CameraPosition) {
    super.onMapCameraMoved(position)
    viewModel.updateCameraPosition(position)
  }

  companion object {
    fun newInstance(viewModel: DropAPinTaskViewModel, map: MapFragment) =
      DropAPinMapFragment(viewModel).apply { this.map = map }
  }
}
