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
import com.google.android.ground.databinding.SyncStatusFragBinding
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.util.setComposableContent
import dagger.hilt.android.AndroidEntryPoint

/**
 * This fragment summarizes the synchronization statuses of local changes that are being uploaded to
 * a remote server.
 */
@AndroidEntryPoint
class SyncStatusFragment : AbstractFragment() {

  lateinit var viewModel: SyncStatusViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(SyncStatusViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    val binding = SyncStatusFragBinding.inflate(inflater, container, false)
    binding.viewModel = viewModel
    binding.lifecycleOwner = this
    binding.composeView.setComposableContent { ShowSyncItems() }
    getAbstractActivity().setSupportActionBar(binding.syncStatusToolbar)
    return binding.root
  }

  @Composable
  private fun ShowSyncItems() {
    val list by viewModel.uploadStatus.observeAsState()
    list?.let {
      LazyColumn(Modifier.fillMaxSize().testTag("sync list")) {
        items(it) {
          SyncListItem(modifier = Modifier.semantics { testTag = "item ${it.user}" }, detail = it)
        }
      }
    }
  }
}
