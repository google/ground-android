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
import com.google.android.ground.ui.common.AbstractMapFragmentWithControls
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.datacollection.DataCollectionFragment
import com.google.android.ground.ui.map.MapFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class CaptureLocationTaskMapFragment : AbstractMapFragmentWithControls() {

  private lateinit var mapViewModel: CaptureLocationTaskMapViewModel
  private lateinit var viewModel: CaptureLocationTaskViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mapViewModel = getViewModel(CaptureLocationTaskMapViewModel::class.java)
    arguments?.let {
      try {
        val taskId = it.getString("taskId")
        taskId?.let {
          viewModel =
            (requireParentFragment() as DataCollectionFragment).viewModel.getTaskViewModel(taskId)
              as CaptureLocationTaskViewModel
        }
      } catch (e: Exception) {
        Timber.e("CaptureLocationTaskMapFragment - $e")
      }
    }
  }

  override fun getMapViewModel(): BaseMapViewModel = mapViewModel

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    val root = super.onCreateView(inflater, container, savedInstanceState)
    viewLifecycleOwner.lifecycleScope.launch {
      if (this@CaptureLocationTaskMapFragment::viewModel.isInitialized) {
        getMapViewModel().getLocationUpdates().collect { viewModel.updateLocation(it) }
      }
    }
    return root
  }

  override fun onMapReady(map: MapFragment) {
    binding.locationLockBtn.isClickable = false
    viewLifecycleOwner.lifecycleScope.launch {
      if (this@CaptureLocationTaskMapFragment::viewModel.isInitialized) {
        viewModel.onMapReady(mapViewModel)
      }
    }
  }

  companion object {
    fun newInstance(map: MapFragment, taskId: String): CaptureLocationTaskMapFragment {
      val fragment = CaptureLocationTaskMapFragment().apply { this.map = map }
      val args = Bundle().apply { putString("taskId", taskId) }
      fragment.arguments = args
      return fragment
    }
  }
}
