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
package org.groundplatform.android.ui.offlineareas.viewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.databinding.BasemapLayoutBinding
import org.groundplatform.android.ui.common.AbstractMapContainerFragment
import org.groundplatform.android.ui.common.BaseMapViewModel
import org.groundplatform.android.ui.common.EphemeralPopups
import org.groundplatform.android.ui.map.MapFragment
import org.groundplatform.android.util.setComposableContent
import org.groundplatform.domain.model.map.MapType
import org.groundplatform.ui.map.MapConfig

/** The fragment provides a UI for managing a single offline area on the user's device. */
@AndroidEntryPoint
class OfflineAreaViewerFragment @Inject constructor() : AbstractMapContainerFragment() {

  @Inject lateinit var popups: EphemeralPopups
  private lateinit var viewModel: OfflineAreaViewerViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val args = OfflineAreaViewerFragmentArgs.fromBundle(requireArguments())
    viewModel = getViewModel(OfflineAreaViewerViewModel::class.java)
    viewModel.initialize(args.offlineAreaId)
  }

  override fun getMapConfig(): MapConfig =
    super.getMapConfig()
      .copy(allowGestures = false, overrideMapType = MapType.TERRAIN, showOfflineImagery = true)

  override fun onMapReady(map: MapFragment) {
    super.onMapReady(map)
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState
          .mapNotNull { it.area }
          .distinctUntilChanged()
          .collect { map.viewport = it.bounds }
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    val binding = BasemapLayoutBinding.inflate(inflater, container, false)
    binding.composeContent.apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setComposableContent {
        OfflineAreaViewerScreen(
          viewModel = viewModel,
          onNavigateUp = { findNavController().navigateUp() },
        )
      }
    }
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiEvent.collect { event ->
          when (event) {
            OfflineAreaViewerEvent.NavigateUp -> {
              if (findNavController().currentDestination?.id == R.id.offline_area_viewer_fragment) {
                findNavController().navigateUp()
              }
            }
            OfflineAreaViewerEvent.RemoveFailed -> {
              popups.ErrorPopup().unknownError()
            }
          }
        }
      }
    }
  }

  override fun getMapViewModel(): BaseMapViewModel = viewModel
}
