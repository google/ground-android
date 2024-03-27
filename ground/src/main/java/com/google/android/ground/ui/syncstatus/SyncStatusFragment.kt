/*
 * Copyright 2021 Google LLC
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
package com.google.android.ground.ui.syncstatus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.ground.databinding.SyncStatusFragBinding
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.common.LocationOfInterestHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Fragment containing a list of mutations and their respective upload statuses. */
@AndroidEntryPoint
class SyncStatusFragment : AbstractFragment() {

  @Inject
  lateinit var locationOfInterestHelper: LocationOfInterestHelper
  lateinit var viewModel: SyncStatusViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(SyncStatusViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    val binding = SyncStatusFragBinding.inflate(inflater, container, false)
    binding.viewModel = viewModel
    binding.lifecycleOwner = this

    getAbstractActivity().setSupportActionBar(binding.syncStatusToolbar)

    val syncStatusListAdapter = SyncStatusListAdapter(requireContext())
    val recyclerView = binding.syncStatusList
    recyclerView.setHasFixedSize(true)
    recyclerView.layoutManager = LinearLayoutManager(context)
    recyclerView.adapter = syncStatusListAdapter

    viewModel.mutations.observe(viewLifecycleOwner) { syncStatusListAdapter.update(it) }
    return binding.root
  }
}
