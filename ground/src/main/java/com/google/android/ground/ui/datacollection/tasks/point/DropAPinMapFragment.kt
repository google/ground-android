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
import com.google.android.ground.databinding.MapTaskFragBinding
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.submission.GeometryData
import com.google.android.ground.ui.common.AbstractMapContainerFragment
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.datacollection.tasks.point.LatLngConverter.processCoordinate
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.MapFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint(AbstractMapContainerFragment::class)
class DropAPinMapFragment(private val viewModel: DropAPinTaskViewModel) :
  Hilt_DropAPinMapFragment() {

  private lateinit var binding: MapTaskFragBinding
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
    binding = MapTaskFragBinding.inflate(inflater, container, false)
    binding.fragment = this
    binding.viewModel = mapViewModel
    binding.lifecycleOwner = this

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        mapViewModel.locationAccuracy.collect { setLocationAccuracyAsInfoCard(it) }
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.taskDataValue.collect {
          setDroppedPinAsInfoCard((it as? GeometryData)?.geometry as? Point)
        }
      }
    }

    return binding.root
  }

  private fun setLocationAccuracyAsInfoCard(locationAccuracy: Float?) {
    if (locationAccuracy == null) {
      binding.infoCard.visibility = View.GONE
    } else {
      binding.cardTitle.setText(R.string.accuracy)
      binding.cardValue.text = getString(R.string.location_accuracy, locationAccuracy)
      binding.infoCard.visibility = View.VISIBLE
    }
  }

  private fun setDroppedPinAsInfoCard(point: Point?) {
    if (point == null) {
      binding.infoCard.visibility = View.GONE
    } else {
      binding.cardTitle.setText(R.string.dropped_pin)
      binding.cardValue.text = processCoordinate(point.coordinate)
      binding.infoCard.visibility = View.VISIBLE
    }
  }

  override fun onMapReady(mapFragment: MapFragment) {
    viewModel.features.observe(this) { mapFragment.renderFeatures(it) }
  }

  override fun getMapViewModel(): BaseMapViewModel = mapViewModel

  override fun onMapCameraMoved(position: CameraPosition) {
    super.onMapCameraMoved(position)
    viewModel.updateCameraPosition(position)
  }

  companion object {
    fun newInstance(viewModel: DropAPinTaskViewModel, mapFragment: MapFragment) =
      DropAPinMapFragment(viewModel).apply { this.mapFragment = mapFragment }
  }
}
