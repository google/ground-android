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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.model.map.MapType
import org.groundplatform.android.ui.common.AbstractMapContainerFragment
import org.groundplatform.android.ui.common.BaseMapViewModel
import org.groundplatform.android.ui.common.EphemeralPopups
import org.groundplatform.android.ui.common.MapConfig
import org.groundplatform.android.ui.home.mapcontainer.HomeScreenMapContainerViewModel
import org.groundplatform.android.ui.map.MapFragment

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
    val root = android.widget.FrameLayout(requireContext())
    root.layoutParams =
      ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
      )

    val mapContainer = androidx.fragment.app.FragmentContainerView(requireContext())
    mapContainer.id = R.id.map
    root.addView(
      mapContainer,
      android.widget.FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
      ),
    )

    val composeView = androidx.compose.ui.platform.ComposeView(requireContext())
    composeView.setViewCompositionStrategy(
      androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
    )
    composeView.setContent {
      org.groundplatform.android.ui.theme.AppTheme {
        val locationLockIcon by viewModel.locationLockIconType.collectAsStateWithLifecycle()
        val bottomText by viewModel.bottomText.observeAsState("")
        val downloadEnabled by viewModel.downloadButtonEnabled.observeAsState(false)
        val showProgress by viewModel.isDownloadProgressVisible.observeAsState(false)
        val progress by viewModel.downloadProgress.observeAsState(0f)

        OfflineAreaSelectorScreen(
          downloadEnabled = downloadEnabled,
          onDownloadClick = { viewModel.onDownloadClick() },
          onCancelClick = { viewModel.onCancelClick() },
          onStopDownloadClick = { viewModel.stopDownloading() },
          onLocationLockClick = { viewModel.onLocationLockClick() },
          locationLockIcon = locationLockIcon,
          bottomText = bottomText.toString(),
          showProgressDialog = showProgress,
          downloadProgress = progress,
          mapView = {},
        )
      }
    }
    root.addView(
      composeView,
      android.widget.FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
      ),
    )
    return root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewLifecycleOwner.lifecycleScope.launch {
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
}
