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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.ground.R
import com.google.android.ground.databinding.MapTaskFragBinding
import com.google.android.ground.ui.common.AbstractMapContainerFragment
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.MapUi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint(AbstractMapContainerFragment::class)
class PolygonDrawingMapFragment(private val viewModel: PolygonDrawingViewModel) :
  Hilt_PolygonDrawingMapFragment() {

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
        mapViewModel.locationAccuracy.collect { updateInfoCard(it) }
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        mapViewModel.getCurrentCameraPosition().collect { onMapCameraMoved(it) }
      }
    }

    return binding.root
  }

  private fun updateInfoCard(locationAccuracy: Float?) {
    if (locationAccuracy == null) {
      binding.infoCard.visibility = View.GONE
    } else {
      binding.cardTitle.setText(R.string.accuracy)
      binding.cardValue.text = getString(R.string.location_accuracy, locationAccuracy)
      binding.infoCard.visibility = View.VISIBLE
    }
  }

  override fun onMapReady(map: MapUi) {
    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.featureValue.collect { feature: Feature? ->
        map.renderFeatures(if (feature == null) setOf() else setOf(feature))
      }
    }
  }

  override fun getMapViewModel(): BaseMapViewModel = mapViewModel

  fun onMapCameraMoved(position: CameraPosition) {
    if (!viewModel.isMarkedComplete()) {
      val mapCenter = position.target!!
      viewModel.updateLastVertexAndMaybeCompletePolygon(mapCenter) { c1, c2 ->
        map.getDistanceInPixels(c1, c2)
      }
    }
  }

  companion object {
    fun newInstance(viewModel: PolygonDrawingViewModel, map: MapUi) =
      PolygonDrawingMapFragment(viewModel).apply { this.map = map }
  }
}
