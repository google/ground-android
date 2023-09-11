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

package com.google.android.ground.ui.offlinebasemap.selector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.ground.databinding.OfflineAreaSelectorFragBinding
import com.google.android.ground.ui.common.AbstractMapContainerFragment
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.common.EphemeralPopups
import com.google.android.ground.ui.common.MapConfig
import com.google.android.ground.ui.map.Map
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Map UI used to select areas for download and viewing offline. */
@AndroidEntryPoint(AbstractMapContainerFragment::class)
class OfflineAreaSelectorFragment : Hilt_OfflineAreaSelectorFragment() {

  @Inject lateinit var popups: EphemeralPopups

  private lateinit var viewModel: OfflineAreaSelectorViewModel

  private var downloadProgressDialogFragment = DownloadProgressDialogFragment()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(OfflineAreaSelectorViewModel::class.java)
    viewModel.isDownloadProgressVisible.observe(this) {
      downloadProgressDialogFragment.setVisibility(childFragmentManager, it)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    val binding = OfflineAreaSelectorFragBinding.inflate(inflater, container, false)
    binding.viewModel = viewModel
    binding.lifecycleOwner = this
    getAbstractActivity().setActionBar(binding.offlineAreaSelectorToolbar, true)
    return binding.root
  }

  override fun onMapReady(map: Map) = viewModel.onMapReady(map)

  override fun getMapViewModel(): BaseMapViewModel = viewModel

  override fun getMapConfig(): MapConfig = super.getMapConfig().copy(showTileOverlays = false)
}
