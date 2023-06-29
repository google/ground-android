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
package com.google.android.ground.ui.offlinebasemap.viewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.ground.databinding.OfflineBaseMapViewerFragBinding
import com.google.android.ground.model.imagery.OfflineArea
import com.google.android.ground.ui.common.AbstractMapContainerFragment
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.map.Map
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** The fragment provides a UI for managing a single offline area on the user's device. */
@AndroidEntryPoint(AbstractMapContainerFragment::class)
class OfflineAreaViewerFragment @Inject constructor() : Hilt_OfflineAreaViewerFragment() {

  private lateinit var viewModel: OfflineAreaViewerViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val args = OfflineAreaViewerFragmentArgs.fromBundle(requireNotNull(arguments))
    viewModel = getViewModel(OfflineAreaViewerViewModel::class.java)
    viewModel.loadOfflineArea(args)
    viewModel.offlineArea.observe(this) { offlineArea: OfflineArea -> panMap(offlineArea) }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    val binding = OfflineBaseMapViewerFragBinding.inflate(inflater, container, false)
    binding.viewModel = viewModel
    binding.lifecycleOwner = this
    binding.removeButton.setOnClickListener { onRemoveClick() }
    getAbstractActivity().setActionBar(binding.offlineAreaViewerToolbar, true)
    return binding.root
  }

  override fun onMapReady(map: Map) {
    map.disableGestures()
  }

  override fun getMapViewModel(): BaseMapViewModel = viewModel

  private fun panMap(offlineArea: OfflineArea) {
    map.viewport = offlineArea.bounds
  }

  /** Removes the area associated with this fragment from the user's device. */
  private fun onRemoveClick() {
    viewModel.removeArea()
  }
}
