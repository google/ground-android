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
package org.groundplatform.android.ui.datacollection.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.common.Constants.ACCURACY_THRESHOLD_IN_M
import org.groundplatform.android.databinding.MapTaskFragBinding
import org.groundplatform.android.model.map.CameraPosition
import org.groundplatform.android.ui.common.AbstractMapContainerFragment
import org.groundplatform.android.ui.common.BaseMapViewModel
import org.groundplatform.android.ui.components.MapFloatingActionButton
import org.groundplatform.android.ui.components.MapFloatingActionButtonType
import org.groundplatform.android.ui.components.RecenterButton
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.map.Feature
import org.groundplatform.android.ui.map.MapFragment
import org.groundplatform.android.ui.map.gms.getAccuracyOrNull
import org.groundplatform.android.ui.map.gms.toCoordinates
import org.groundplatform.android.util.setComposableContent
import org.groundplatform.android.util.toDmsFormat
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
    setupMapActionButtons()

    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        getMapViewModel().location.collect {
          val locationText = it?.toCoordinates()?.toDmsFormat()

          val df = DecimalFormat("#.##")
          df.roundingMode = RoundingMode.DOWN
          val accuracy = it?.getAccuracyOrNull()
          val accuracyText = accuracy?.let { value -> df.format(value) + "m" } ?: "?"

          updateLocationInfoCard(R.string.current_location, locationText, accuracyText, accuracy)
        }
      }
    }

    return binding.root
  }

  private fun setupMapActionButtons() {
    binding.mapTypeBtn.apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setComposableContent {
        MapFloatingActionButton(
          type = MapFloatingActionButtonType.MapType,
          onClick = { showMapTypeSelectorDialog() },
        )
      }
    }

    binding.locationLockBtn.apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setComposableContent {
        val locationLockButton by viewModel.locationLockIconType.collectAsStateWithLifecycle()

        MapFloatingActionButton(
          type = locationLockButton,
          onClick = { viewModel.onLocationLockClick() },
        )
      }
    }

    binding.recenterBtn.apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setComposableContent {
        val shouldShowRecenter by viewModel.shouldShowRecenterButton.collectAsStateWithLifecycle()

        if (shouldShowRecenter)
          RecenterButton(
            modifier = Modifier.padding(start = 20.dp),
            onClick = { viewModel.onLocationLockClick() },
          )
      }
    }
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
    accuracyInMeters: Double? = null,
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
        val color =
          if (accuracyInMeters == null || accuracyInMeters > ACCURACY_THRESHOLD_IN_M) {
            R.color.accuracy_bad
          } else {
            R.color.accuracy_good
          }
        accuracyValue.setTextColor(resources.getColor(color, null))
      }
    }

  fun setCenterMarkerVisibility(visible: Boolean) {
    binding.centerMarker.visibility = if (visible) View.VISIBLE else View.GONE
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
