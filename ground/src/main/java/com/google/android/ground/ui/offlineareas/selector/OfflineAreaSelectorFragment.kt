/*
 * Copyright 2020 Google LLC
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

package com.google.android.ground.ui.offlineareas.selector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.ground.databinding.OfflineAreaSelectorFragBinding
import com.google.android.ground.ui.common.AbstractMapContainerFragment
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.home.mapcontainer.HomeScreenMapContainerViewModel
import com.google.android.ground.ui.map.MapFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Map UI used to select areas for download and viewing offline. */
@AndroidEntryPoint
class OfflineAreaSelectorFragment : AbstractMapContainerFragment() {

  private lateinit var viewModel: OfflineAreaSelectorViewModel
  private lateinit var mapContainerViewModel: HomeScreenMapContainerViewModel

  private var downloadProgressDialogFragment: DownloadProgressDialogFragment? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mapContainerViewModel = getViewModel(HomeScreenMapContainerViewModel::class.java)
    viewModel = getViewModel(OfflineAreaSelectorViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    val binding = OfflineAreaSelectorFragBinding.inflate(inflater, container, false)
    binding.viewModel = viewModel
    binding.lifecycleOwner = this
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    viewModel.isDownloadProgressVisible.observe(viewLifecycleOwner) {
      if (downloadProgressDialogFragment == null) {
        downloadProgressDialogFragment = DownloadProgressDialogFragment()
      }
      downloadProgressDialogFragment?.setVisibility(childFragmentManager, it)
    }
  }

  override fun onMapReady(map: MapFragment) {
    // Observe events emitted by the ViewModel.
    viewLifecycleOwner.lifecycleScope.launch {
      mapContainerViewModel.mapLoiFeatures.collect { map.setFeatures(it) }
    }
    map.addTileOverlay(viewModel.remoteTileSource)
  }

  override fun getMapViewModel(): BaseMapViewModel = viewModel
}
