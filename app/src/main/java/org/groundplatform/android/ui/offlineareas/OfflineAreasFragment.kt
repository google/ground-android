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
package org.groundplatform.android.ui.offlineareas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.databinding.OfflineAreasFragBinding
import org.groundplatform.android.ui.common.AbstractFragment
import org.groundplatform.android.util.setComposableContent

/**
 * Fragment containing a list of downloaded areas on the device. An area is a set of offline raster
 * tiles. Users can manage their areas within this fragment. They can delete areas they no longer
 * need or access the UI used to select and download a new area to the device.
 */
@AndroidEntryPoint
class OfflineAreasFragment : AbstractFragment() {

  private lateinit var viewModel: OfflineAreasViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(OfflineAreasViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    val binding = OfflineAreasFragBinding.inflate(inflater, container, false)
    binding.viewModel = viewModel
    binding.lifecycleOwner = this
    binding.offlineAreasListComposeView.setComposableContent { ShowOfflineAreas() }

    getAbstractActivity().setSupportActionBar(binding.offlineAreasToolbar)

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    lifecycleScope.launch {
      viewModel.navigateToOfflineAreaSelector.collectLatest {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.offline_areas_fragment) {
          navController.navigate(OfflineAreasFragmentDirections.showOfflineAreaSelector())
        }
      }
    }
  }

  @Composable
  private fun ShowOfflineAreas() {
    val list by viewModel.offlineAreas.observeAsState()
    list?.let {
      LazyColumn(Modifier.fillMaxSize().testTag("offline area list")) {
        items(it) {
          OfflineAreaListItem(offlineAreaDetails = it) { areaId ->
            findNavController().navigate(OfflineAreasFragmentDirections.viewOfflineArea(areaId))
          }
        }
      }
    }
  }
}
