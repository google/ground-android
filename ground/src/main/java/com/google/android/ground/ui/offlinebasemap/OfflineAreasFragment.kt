/*
 * Copyright 2019 Google LLC
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
package com.google.android.ground.ui.offlinebasemap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.ground.MainActivity
import com.google.android.ground.databinding.OfflineBaseMapsFragBinding
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.common.Navigator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Fragment containing a list of downloaded areas on the device. An area is a set of offline raster
 * tiles. Users can manage their areas within this fragment. They can delete areas they no longer
 * need or access the UI used to select and download a new area to the device.
 */
@AndroidEntryPoint
class OfflineAreasFragment : AbstractFragment() {

  @Inject lateinit var navigator: Navigator

  private lateinit var offlineAreaListAdapter: OfflineAreaListAdapter
  private lateinit var viewModel: OfflineAreasViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(OfflineAreasViewModel::class.java)
    offlineAreaListAdapter = OfflineAreaListAdapter(navigator)
    viewModel.offlineAreas.observe(this) { offlineAreaListAdapter.update(it) }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    val binding = OfflineBaseMapsFragBinding.inflate(inflater, container, false)
    binding.viewModel = viewModel
    binding.lifecycleOwner = this

    (requireActivity() as MainActivity).setActionBar(binding.offlineAreasToolbar, true)

    val recyclerView = binding.offlineAreasList
    recyclerView.setHasFixedSize(true)
    recyclerView.layoutManager = LinearLayoutManager(context)
    recyclerView.adapter = offlineAreaListAdapter
    return binding.root
  }
}
