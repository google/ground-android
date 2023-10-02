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
package com.google.android.ground.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.ground.R
import com.google.android.ground.databinding.MapTaskFragBinding
import com.google.android.ground.model.submission.LocationTaskData.Companion.toTaskData
import com.google.android.ground.ui.datacollection.tasks.point.LatLngConverter
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.MapFragment
import kotlinx.coroutines.launch
import org.jetbrains.annotations.MustBeInvokedByOverriders

/**
 * Injects a [MapFragment] in the container with id "map" and provides shared map functionality
 * including map controls like "MapType" button, "CurrentLocation" button and info card.
 */
abstract class AbstractMapFragmentWithControls : AbstractMapContainerFragment() {

  protected lateinit var binding: MapTaskFragBinding

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = MapTaskFragBinding.inflate(inflater, container, false)
    binding.fragment = this
    binding.viewModel = getMapViewModel()
    binding.lifecycleOwner = this

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        getMapViewModel().location.collect {
          val locationText = it?.toTaskData()?.getDetailsText()
          setCurrentLocationAsInfoCard(locationText)
        }
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        getMapViewModel().getCurrentCameraPosition().collect { onMapCameraMoved(it) }
      }
    }

    return binding.root
  }

  private fun setCurrentLocationAsInfoCard(locationText: String?) {
    if (locationText.isNullOrEmpty()) {
      binding.infoCard.visibility = View.GONE
    } else {
      binding.cardTitle.setText(R.string.current_location)
      binding.cardValue.text = locationText
      binding.infoCard.visibility = View.VISIBLE
    }
  }

  @MustBeInvokedByOverriders
  protected open fun onMapCameraMoved(position: CameraPosition) {
    if (getMapViewModel().locationLock.value.getOrDefault(false)) {
      // Don't update the info card as it is already showing current location
      return
    }
    val target = position.target
    val processedCoordinates = LatLngConverter.formatCoordinates(target)
    setCurrentLocationAsInfoCard(processedCoordinates)
  }
}
