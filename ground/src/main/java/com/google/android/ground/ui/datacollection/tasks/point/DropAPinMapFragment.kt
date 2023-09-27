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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.ground.R
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.submission.GeometryData
import com.google.android.ground.ui.common.AbstractMapFragmentWithControls
import com.google.android.ground.ui.datacollection.components.TaskHeaderPopupView
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.MapFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint(AbstractMapFragmentWithControls::class)
class DropAPinMapFragment(private val viewModel: DropAPinTaskViewModel) :
  Hilt_DropAPinMapFragment() {

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

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.taskDataValue.collect {
          setDroppedPinAsInfoCard((it as? GeometryData)?.geometry as? Point, R.string.dropped_pin)
        }
      }
    }

    return root
  }

  override fun onMapReady(map: MapFragment) {
    viewModel.features.observe(this) { map.renderFeatures(it) }
  }

  override fun onMapCameraMoved(position: CameraPosition) {
    viewModel.updateCameraPosition(position)
  }

  companion object {
    fun newInstance(viewModel: DropAPinTaskViewModel, map: MapFragment) =
      DropAPinMapFragment(viewModel).apply { this.map = map }
  }
}
