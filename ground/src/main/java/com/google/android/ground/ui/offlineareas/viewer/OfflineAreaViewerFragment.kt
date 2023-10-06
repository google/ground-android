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
package com.google.android.ground.ui.offlineareas.viewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.ground.databinding.OfflineAreaViewerFragBinding
import com.google.android.ground.ui.common.AbstractMapContainerFragment
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.map.MapFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/** The fragment provides a UI for managing a single offline area on the user's device. */
@AndroidEntryPoint(AbstractMapContainerFragment::class)
class OfflineAreaViewerFragment @Inject constructor() : Hilt_OfflineAreaViewerFragment() {

  private lateinit var viewModel: OfflineAreaViewerViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val args = OfflineAreaViewerFragmentArgs.fromBundle(requireNotNull(arguments))
    viewModel = getViewModel(OfflineAreaViewerViewModel::class.java)
    viewModel.initialize(args)
  }

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
    savedInstanceState: Bundle?
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    val binding = OfflineAreaViewerFragBinding.inflate(inflater, container, false)
    binding.viewModel = viewModel
    binding.lifecycleOwner = this
    getAbstractActivity().setActionBar(binding.offlineAreaViewerToolbar, true)
    return binding.root
  }

  override fun getMapViewModel(): BaseMapViewModel = viewModel
}
