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

package org.groundplatform.android.ui.offlineareas.selector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.databinding.OfflineAreaSelectorFragBinding
import org.groundplatform.android.ui.common.AbstractMapContainerFragment
import org.groundplatform.android.ui.common.BaseMapViewModel
import org.groundplatform.android.ui.common.EphemeralPopups
import org.groundplatform.android.ui.common.MapConfig
import org.groundplatform.android.ui.home.mapcontainer.HomeScreenMapContainerViewModel
import org.groundplatform.android.ui.map.MapFragment
import org.groundplatform.android.ui.map.MapType
import org.groundplatform.android.util.renderComposableDialog

/** Map UI used to select areas for download and viewing offline. */
@AndroidEntryPoint
class OfflineAreaSelectorFragment : AbstractMapContainerFragment() {

  private lateinit var viewModel: OfflineAreaSelectorViewModel
  private lateinit var mapContainerViewModel: HomeScreenMapContainerViewModel

  @Inject lateinit var popups: EphemeralPopups

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
    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.isDownloadProgressVisible.observe(viewLifecycleOwner) {
        showDownloadProgressDialog(it)
      }
      viewModel.isFailure.observe(viewLifecycleOwner) {
        if (it) {
          Toast.makeText(context, R.string.offline_area_download_error, Toast.LENGTH_LONG).show()
        }
      }
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

    lifecycleScope.launch {
      viewModel.networkUnavailableEvent.collect {
        popups.ErrorPopup().show(R.string.connect_to_download_message)
      }
    }
  }

  override fun getMapConfig(): MapConfig =
    super.getMapConfig()
      .copy(
        allowRotateGestures = false,
        overrideMapType = MapType.TERRAIN,
        showOfflineImagery = false,
      )

  override fun onMapReady(map: MapFragment) {
    // Observe events emitted by the ViewModel.
    viewLifecycleOwner.lifecycleScope.launch {
      mapContainerViewModel.mapLoiFeatures.collect { map.setFeatures(it) }
    }
    map.addTileOverlay(viewModel.remoteTileSource)
  }

  override fun getMapViewModel(): BaseMapViewModel = viewModel

  private fun showDownloadProgressDialog(isVisible: Boolean) {
    renderComposableDialog {
      val openAlertDialog = remember { mutableStateOf(isVisible) }
      val progress = viewModel.downloadProgress.observeAsState(0f)
      when {
        openAlertDialog.value -> {
          DownloadProgressDialog(
            progress = progress.value,
            onDismiss = {
              openAlertDialog.value = false
              viewModel.stopDownloading()
            },
          )
        }
      }
    }
  }
}
