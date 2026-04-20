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
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.databinding.OfflineAreaSelectorFragBinding
import org.groundplatform.domain.model.map.MapType
import org.groundplatform.android.ui.common.AbstractMapContainerFragment
import org.groundplatform.android.ui.common.BaseMapViewModel
import org.groundplatform.android.ui.common.EphemeralPopups
import org.groundplatform.android.ui.common.MapConfig
import org.groundplatform.android.ui.components.MapFloatingActionButton
import org.groundplatform.android.ui.home.mapcontainer.HomeScreenMapContainerViewModel
import org.groundplatform.android.ui.map.MapFragment
import org.groundplatform.android.ui.offlineareas.selector.model.OfflineAreaSelectorEvent
import org.groundplatform.android.ui.offlineareas.selector.model.OfflineAreaSelectorState
import org.groundplatform.android.util.renderComposableDialog
import org.groundplatform.android.util.setComposableContent

/** Map UI used to select areas for download and viewing offline. */
@AndroidEntryPoint
class OfflineAreaSelectorFragment : AbstractMapContainerFragment() {

  private lateinit var viewModel: OfflineAreaSelectorViewModel
  private lateinit var mapContainerViewModel: HomeScreenMapContainerViewModel

  @Inject lateinit var popups: EphemeralPopups

  private lateinit var binding: OfflineAreaSelectorFragBinding

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
    binding = OfflineAreaSelectorFragBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
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
    binding.downloadButton.setOnClickListener { viewModel.onDownloadClick() }
    binding.cancelButton.setOnClickListener { viewModel.onCancelClick() }
    setupDownloadProgressDialog()
    setupObservers()
  }

  private fun setupObservers() {
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { viewModel.uiState.collect { updateUi(it) } }

        launch {
          viewModel.uiEvent.collect {
            when (it) {
              is OfflineAreaSelectorEvent.NavigateOfflineAreaBackToHomeScreen -> {
                findNavController()
                  .navigate(OfflineAreaSelectorFragmentDirections.offlineAreaBackToHomescreen())
              }

              is OfflineAreaSelectorEvent.NavigateUp -> {
                findNavController().navigateUp()
              }

              OfflineAreaSelectorEvent.NetworkUnavailable -> {
                popups.ErrorPopup().show(R.string.connect_to_download_message)
              }

              OfflineAreaSelectorEvent.DownloadError -> {
                Toast.makeText(context, R.string.offline_area_download_error, Toast.LENGTH_LONG)
                  .show()
              }
            }
          }
        }
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

  private fun updateUi(state: OfflineAreaSelectorState) {
    binding.bottomText.text =
      when (state.bottomTextState) {
        is OfflineAreaSelectorState.BottomTextState.AreaSize ->
          resources.getString(R.string.selected_offline_area_size, state.bottomTextState.size)

        OfflineAreaSelectorState.BottomTextState.AreaTooLarge ->
          resources.getString(R.string.selected_offline_area_too_large)

        OfflineAreaSelectorState.BottomTextState.Loading ->
          resources.getString(
            R.string.selected_offline_area_size,
            resources.getString(R.string.offline_area_size_loading_symbol),
          )

        OfflineAreaSelectorState.BottomTextState.NetworkError ->
          resources.getString(R.string.connect_to_download_message)

        OfflineAreaSelectorState.BottomTextState.NoImageryAvailable ->
          resources.getString(R.string.no_imagery_available_for_area)

        null -> ""
      }

    with(binding.downloadButton) {
      isEnabled = state.isDownloadButtonEnabled()
      isClickable = state.isDownloadButtonEnabled()
    }
  }

  private fun setupDownloadProgressDialog() {
    renderComposableDialog {
      val state by viewModel.uiState.collectAsStateWithLifecycle()
      val downloadState = state.downloadState
      if (downloadState is OfflineAreaSelectorState.DownloadState.InProgress) {
        DownloadProgressDialog(
          progress = downloadState.progress,
          onDismiss = { viewModel.stopDownloading() },
        )
      }
    }
  }
}
