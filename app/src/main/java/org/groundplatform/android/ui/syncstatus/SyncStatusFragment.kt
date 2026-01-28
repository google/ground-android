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
package org.groundplatform.android.ui.syncstatus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.groundplatform.android.ui.common.AbstractFragment

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
    return androidx.compose.ui.platform.ComposeView(requireContext()).apply {
      setViewCompositionStrategy(
        androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
      )
      setContent {
        org.groundplatform.android.ui.theme.AppTheme {
          val list by viewModel.uploadStatus.observeAsState(emptyList())
          SyncStatusScreen(uploadStatuses = list, onBack = { findNavController().navigateUp() })
        }
      }
    }
  }
}
