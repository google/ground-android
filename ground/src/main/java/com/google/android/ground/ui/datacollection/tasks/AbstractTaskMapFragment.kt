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
package com.google.android.ground.ui.datacollection.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.ground.R
import com.google.android.ground.databinding.MapTaskFragBinding
import com.google.android.ground.ui.common.AbstractMapContainerFragment
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.datacollection.DataCollectionViewModel
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.MapFragment
import com.google.android.ground.ui.map.gms.getAccuracyOrNull
import com.google.android.ground.ui.map.gms.toCoordinates
import com.google.android.ground.util.toDmsFormat
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlinx.coroutines.launch
import org.jetbrains.annotations.MustBeInvokedByOverriders

abstract class AbstractTaskMapFragment<TVM : AbstractTaskViewModel> :
  AbstractMapContainerFragment() {

  protected lateinit var binding: MapTaskFragBinding

  protected val dataCollectionViewModel: DataCollectionViewModel by
    hiltNavGraphViewModels(R.id.data_collection)

  protected val taskViewModel: TVM by lazy {
    // Access to this viewModel is lazy for testing. This is because the NavHostController could
    // not be initialized before the Fragment under test is created, leading to
    // hiltNavGraphViewModels() to fail when called on launch.
    dataCollectionViewModel.getTaskViewModel(taskId) as TVM
  }

  private lateinit var viewModel: BaseMapViewModel

  protected val taskId: String by lazy {
    arguments?.getString(TASK_ID_FRAGMENT_ARG_KEY) ?: error("null taskId fragment arg")
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(BaseMapViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    binding = MapTaskFragBinding.inflate(inflater, container, false)
    binding.fragment = this
    binding.viewModel = getMapViewModel()
    binding.lifecycleOwner = this

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        getMapViewModel().location.collect {
          val locationText = it?.toCoordinates()?.toDmsFormat()

          val df = DecimalFormat("#.##")
          df.roundingMode = RoundingMode.DOWN
          val accuracyText = it?.getAccuracyOrNull()?.let { value -> df.format(value) + "m" } ?: "?"

          updateLocationInfoCard(R.string.current_location, locationText, accuracyText)
        }
      }
    }

    return binding.root
  }

  override fun getMapViewModel(): BaseMapViewModel = viewModel

  @MustBeInvokedByOverriders
  override fun onMapReady(map: MapFragment) {
    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        getMapViewModel().getCurrentCameraPosition().collect { onMapCameraMoved(it) }
      }
    }

    renderFeatures().observe(this) { map.setFeatures(it) }

    // Allow the fragment to restore map viewport to previously drawn feature.
    setDefaultViewPort()
  }

  /** Must be overridden by subclasses. */
  open fun renderFeatures(): LiveData<Set<Feature>> = MutableLiveData(setOf())

  /**
   * This should be overridden if the fragment wants to set a custom map camera position. Default
   * behavior is to move the camera to the last known position.
   */
  open fun setDefaultViewPort() {}

  private fun updateLocationInfoCard(
    @StringRes title: Int,
    locationText: String?,
    accuracyText: String? = null,
  ) =
    with(binding) {
      if (locationText.isNullOrEmpty()) {
        infoCard.visibility = View.GONE
      } else {
        infoCard.visibility = View.VISIBLE
        currentLocationTitle.text = getString(title)
        currentLocationValue.text = locationText
      }

      if (accuracyText.isNullOrEmpty()) {
        accuracy.visibility = View.GONE
      } else {
        accuracy.visibility = View.VISIBLE
        accuracyTitle.setText(R.string.accuracy)
        accuracyValue.text = accuracyText
      }
    }

  @MustBeInvokedByOverriders
  protected open fun onMapCameraMoved(position: CameraPosition) {
    if (getMapViewModel().locationLock.value.getOrDefault(false)) {
      // Don't update the info card as it is already showing current location
      return
    }
    updateLocationInfoCard(R.string.map_location, position.coordinates.toDmsFormat())
  }

  companion object {
    const val TASK_ID_FRAGMENT_ARG_KEY = "taskId"
  }
}
