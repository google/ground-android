/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.android.ui.startup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.ui.components.LoadingDialog

/**
 * Displays the startup screen and handles initial application setup.
 *
 * @param onLoadFailed callback invoked when the startup initialization process fails.
 * @param viewModel the [StartupViewModel] responsible for managing the startup state.
 */
@Composable
fun StartupScreen(
  onLoadFailed: (errorMessageId: Int?) -> Unit,
  viewModel: StartupViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  Box(modifier = Modifier.fillMaxSize()) {
    when (state) {
      is StartupState.Loading -> LoadingDialog(messageId = R.string.initializing)
      is StartupState.Error -> onLoadFailed((state as StartupState.Error).errorMessageId)
    }
  }
}
