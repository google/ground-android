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
package org.groundplatform.android.ui.offlineareas.selector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.groundplatform.android.R
import org.groundplatform.android.ui.components.MapFloatingActionButton
import org.groundplatform.android.ui.components.MapFloatingActionButtonType

private val TOP_MASK_HEIGHT = 24.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod")
fun OfflineAreaSelectorScreen(
  downloadEnabled: Boolean,
  onDownloadClick: () -> Unit,
  onCancelClick: () -> Unit,
  onStopDownloadClick: () -> Unit,
  onLocationLockClick: () -> Unit,
  locationLockIcon: MapFloatingActionButtonType,
  bottomText: String,
  showProgressDialog: Boolean,
  downloadProgress: Float,
  mapView: @Composable () -> Unit,
) {
  Scaffold(
    topBar = { TopAppBar(title = { Text(stringResource(R.string.offline_area_selector_title)) }) }
  ) { paddingValues ->
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
      // Map (Background)
      Box(modifier = Modifier.fillMaxSize()) { mapView() }

      // Overlays to create Viewport "Hole"
      OfflineAreaSelectorOverlay(
        locationLockIcon = locationLockIcon,
        onLocationLockClick = onLocationLockClick,
        bottomText = bottomText,
        onCancelClick = onCancelClick,
        onDownloadClick = onDownloadClick,
        downloadEnabled = downloadEnabled,
      )
    }

    if (showProgressDialog) {
      OfflineAreaProgressDialog(
        downloadProgress = downloadProgress,
        onStopDownloadClick = onStopDownloadClick,
      )
    }
  }
}

@Composable
private fun OfflineAreaSelectorOverlay(
  locationLockIcon: MapFloatingActionButtonType,
  onLocationLockClick: () -> Unit,
  bottomText: String,
  onCancelClick: () -> Unit,
  onDownloadClick: () -> Unit,
  downloadEnabled: Boolean,
) {
  Column(modifier = Modifier.fillMaxSize()) {
    Box(
      modifier =
        Modifier.fillMaxWidth().height(TOP_MASK_HEIGHT).background(Color.Black.copy(alpha = 0.4f))
    )

    Row(modifier = Modifier.weight(1f)) {
      // Left Mask
      Box(modifier = Modifier.width(24.dp).fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))

      // Viewport Hole (Transparent)
      Box(
        modifier = Modifier.weight(1f).fillMaxSize().border(1.dp, Color.White) // Outline
      ) {
        // Location Lock Button
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
          MapFloatingActionButton(type = locationLockIcon, onClick = onLocationLockClick)
        }
      }

      // Right Mask
      Box(modifier = Modifier.width(24.dp).fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
    }

    // Bottom Mask with Text
    Box(
      modifier = Modifier.fillMaxWidth().height(80.dp).background(Color.Black.copy(alpha = 0.4f)),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = bottomText,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 64.dp),
      )
    }

    OfflineAreaSelectorButtons(
      onCancelClick = onCancelClick,
      onDownloadClick = onDownloadClick,
      downloadEnabled = downloadEnabled,
    )
  }
}

@Composable
private fun OfflineAreaSelectorButtons(
  onCancelClick: () -> Unit,
  onDownloadClick: () -> Unit,
  downloadEnabled: Boolean,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface) // SurfaceContainer in XML
        .padding(16.dp),
    verticalAlignment = Alignment.Top,
  ) {
    OutlinedButton(onClick = onCancelClick, modifier = Modifier.weight(1f)) {
      Text(stringResource(R.string.offline_area_select_cancel_button))
    }
    Spacer(Modifier.width(16.dp))
    Button(onClick = onDownloadClick, enabled = downloadEnabled, modifier = Modifier.weight(1f)) {
      Text(stringResource(R.string.offline_area_selector_download))
    }
  }
}

@Composable
private fun OfflineAreaProgressDialog(downloadProgress: Float, onStopDownloadClick: () -> Unit) {
  AlertDialog(
    onDismissRequest = { /* Prevent dismiss */ },
    title = {
      Text(
        stringResource(
          R.string.offline_map_imagery_download_progress_dialog_title,
          (downloadProgress * 100).toInt(),
        )
      )
    },
    text = {
      Column {
        Text(stringResource(R.string.offline_map_imagery_download_progress_dialog_message))
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(progress = { downloadProgress }, modifier = Modifier.fillMaxWidth())
      }
    },
    confirmButton = {
      Button(onClick = onStopDownloadClick, modifier = Modifier.testTag("CancelProgressButton")) {
        Text(stringResource(R.string.offline_area_select_cancel_button))
      }
    },
  )
}
