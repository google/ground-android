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
package com.google.android.ground.ui.offlineareas

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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import com.google.android.ground.databinding.OfflineAreasFragBinding
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.theme.AppTheme
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
    binding.offlineAreasListComposeView.setContent { AppTheme { ShowOfflineAreas() } }

    getAbstractActivity().setSupportActionBar(binding.offlineAreasToolbar)

    return binding.root
  }

  @Composable
  private fun ShowOfflineAreas() {
    val list by viewModel.offlineAreas.observeAsState()
    list?.let {
      LazyColumn(Modifier.fillMaxSize().testTag("offline area list")) {
        items(it) {
          OfflineAreaListItem(
            modifier = Modifier.semantics { testTag = "item ${it.area.id}" },
            offlineAreaDetails = it,
          ) {
            navigator.navigate(OfflineAreasFragmentDirections.viewOfflineArea(it.area.id))
          }
        }
      }
    }
  }
}
