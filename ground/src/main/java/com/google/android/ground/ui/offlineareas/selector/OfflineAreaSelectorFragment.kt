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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.ground.databinding.OfflineAreaSelectorFragBinding
import com.google.android.ground.ui.common.AbstractMapContainerFragment
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.home.mapcontainer.HomeScreenMapContainerViewModel
import com.google.android.ground.ui.map.MapFragment
import com.google.android.ground.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Map UI used to select areas for download and viewing offline. */
@AndroidEntryPoint
class OfflineAreaSelectorFragment : AbstractMapContainerFragment() {

  private lateinit var viewModel: OfflineAreaSelectorViewModel
  private lateinit var mapContainerViewModel: HomeScreenMapContainerViewModel

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
      showDownloadProgressDialog(it)
    }

    lifecycleScope.launch {
      viewModel.navigate.collect {
        when (it) {
          is UiState.OfflineAreaBackToHomeScreen -> {
            findNavController()
              .navigate(OfflineAreaSelectorFragmentDirections.offlineAreaBackToHomescreen())
          }
          is UiState.Up -> {
            findNavController().navigateUp()
          }
        }
      }
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

  private fun showDownloadProgressDialog(isVisible: Boolean) {
    val dialogComposeView =
      ComposeView(requireContext()).apply {
        setContent {
          val openAlertDialog = remember { mutableStateOf(isVisible) }
          val progress = viewModel.downloadProgress.observeAsState(0f)
          when {
            openAlertDialog.value -> {
              AppTheme {
                DownloadProgressDialog(
                  progress = progress.value,
                  // TODO - Add Download Cancel Feature
                  // https://github.com/google/ground-android/issues/2884
                  onDismiss = { openAlertDialog.value = false },
                )
              }
            }
          }
        }
      }

    (view as ViewGroup).addView(dialogComposeView)
  }
}
