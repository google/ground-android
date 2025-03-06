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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.groundplatform.android.databinding.OfflineAreaViewerFragBinding
import org.groundplatform.android.ui.common.AbstractMapContainerFragment
import org.groundplatform.android.ui.common.BaseMapViewModel
import org.groundplatform.android.ui.common.MapConfig
import org.groundplatform.android.ui.map.MapFragment
import org.groundplatform.android.ui.map.MapType

/** The fragment provides a UI for managing a single offline area on the user's device. */
@AndroidEntryPoint
class OfflineAreaViewerFragment @Inject constructor() : AbstractMapContainerFragment() {

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
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.area.observe(this@OfflineAreaViewerFragment) { map.viewport = it.bounds }
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    val binding = OfflineAreaViewerFragBinding.inflate(inflater, container, false)
    binding.viewModel = viewModel
    binding.lifecycleOwner = this
    getAbstractActivity().setSupportActionBar(binding.offlineAreaViewerToolbar)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    lifecycleScope.launch { viewModel.navigateUp.collect { findNavController().navigateUp() } }
  }

  override fun getMapViewModel(): BaseMapViewModel = viewModel
}
