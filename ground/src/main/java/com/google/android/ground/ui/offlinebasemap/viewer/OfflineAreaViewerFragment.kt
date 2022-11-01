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
import com.google.android.ground.MainActivity
import com.google.android.ground.databinding.OfflineBaseMapViewerFragBinding
import com.google.android.ground.model.basemap.OfflineArea
import com.google.android.ground.ui.common.AbstractMapViewerFragment
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.map.MapFragment
import com.google.android.ground.ui.map.gms.toModelObject
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** The fragment provides a UI for managing a single offline area on the user's device. */
@AndroidEntryPoint
class OfflineAreaViewerFragment @Inject constructor() : AbstractMapViewerFragment() {

  @Inject lateinit var navigator: Navigator

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
    (activity as MainActivity?)!!.setActionBar(binding.offlineAreaViewerToolbar, true)
    return binding.root
  }

  override fun onMapReady(mapFragment: MapFragment) {
    mapFragment.disableGestures()
  }

  private fun panMap(offlineArea: OfflineArea) {
    mapFragment.viewport = offlineArea.bounds.toModelObject()
  }

  /** Removes the area associated with this fragment from the user's device. */
  private fun onRemoveClick() {
    viewModel.removeArea()
  }
}
