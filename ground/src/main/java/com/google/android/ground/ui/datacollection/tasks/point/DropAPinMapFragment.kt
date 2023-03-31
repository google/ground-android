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

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.ground.R
import com.google.android.ground.databinding.MapTaskFragBinding
import com.google.android.ground.model.submission.LocationTaskData
import com.google.android.ground.ui.common.AbstractMapContainerFragment
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.MapFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DropAPinMapFragment(private val viewModel: DropAPinTaskViewModel) :
  AbstractMapContainerFragment() {

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
          setDroppedPinAsInfoCard((it as? LocationTaskData)?.cameraPosition)
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

  private fun setDroppedPinAsInfoCard(cameraPosition: CameraPosition?) {
    if (cameraPosition == null) {
      binding.infoCard.visibility = View.GONE
    } else {
      binding.cardTitle.setText(R.string.dropped_pin)
      binding.cardValue.text = LatLngConverter.processCoordinates(cameraPosition.target)
      binding.infoCard.visibility = View.VISIBLE
    }
  }

  private fun convert(latitude: Double, longitude: Double): String {
    val builder = StringBuilder()
    if (latitude < 0) {
      builder.append("S ")
    } else {
      builder.append("N ")
    }
    val latitudeDegrees = Location.convert(abs(latitude), Location.FORMAT_SECONDS)
    val latitudeSplit =
      latitudeDegrees.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    builder.append(latitudeSplit[0])
    builder.append("°")
    builder.append(latitudeSplit[1])
    builder.append("'")
    builder.append(latitudeSplit[2])
    builder.append("\"")
    builder.append(" ")
    if (longitude < 0) {
      builder.append("W ")
    } else {
      builder.append("E ")
    }
    val longitudeDegrees = Location.convert(abs(longitude), Location.FORMAT_SECONDS)
    val longitudeSplit =
      longitudeDegrees.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    builder.append(longitudeSplit[0])
    builder.append("°")
    builder.append(longitudeSplit[1])
    builder.append("'")
    builder.append(longitudeSplit[2])
    builder.append("\"")
    return builder.toString()
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
