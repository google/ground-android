/*
 * Copyright 2023 Google LLC
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

package com.google.android.ground.ui.offlineareas.selector

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.res.stringResource
import com.google.android.ground.R

@Composable
fun DownloadProgressDialog(viewModel: OfflineAreaSelectorViewModel, onDismiss: () -> Unit) {
  val progressData = viewModel.downloadProgress.observeAsState()

  AlertDialog(
    containerColor = MaterialTheme.colorScheme.surface,
    onDismissRequest = {},
    title = {
      Text(
        stringResource(R.string.offline_map_imagery_download_progress_dialog_title),
        color = MaterialTheme.colorScheme.onSurface,
      )
    },
    text = {
      Column {
        LinearProgressIndicator(
          progress = { progressData.value?.toFloat() ?: 0f },
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text(
          stringResource(R.string.offline_map_imagery_download_progress_dialog_message),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    dismissButton = {
      TextButton(onClick = { onDismiss() }) { Text(text = stringResource(R.string.cancel)) }
    },
    confirmButton = {},
  )
}
