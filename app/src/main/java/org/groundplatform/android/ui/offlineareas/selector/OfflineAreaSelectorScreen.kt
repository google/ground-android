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

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.components.MapFloatingActionButton
import org.groundplatform.android.ui.components.MapFloatingActionButtonType
import org.groundplatform.android.ui.components.Toolbar
import org.groundplatform.ui.theme.AppTheme

const val OFFLINE_AREA_SELECTOR_BOTTOM_TEXT_TEST_TAG = "offline_area_selector_bottom_text"
const val OFFLINE_AREA_SELECTOR_DOWNLOAD_BUTTON_TEST_TAG = "offline_area_selector_download_button"
const val OFFLINE_AREA_SELECTOR_CANCEL_BUTTON_TEST_TAG = "offline_area_selector_cancel_button"
const val DOWNLOAD_PROGRESS_DIALOG_CANCEL_BUTTON_TEST_TAG = "download_progress_dialog_cancel_button"

/**
 * Stateful composable for the Offline Area Selector screen.
 *
 * @param viewModel The view model managing state and actions for offline area selection.
 */
@Composable
fun OfflineAreaSelectorScreen(viewModel: OfflineAreaSelectorViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val locationLockIconType by viewModel.locationLockIconType.collectAsStateWithLifecycle()

  OfflineAreaSelectorScreen(
    uiState = uiState,
    locationLockIconType = locationLockIconType,
    onDownloadClick = { viewModel.onDownloadClick() },
    onCancelClick = { viewModel.onCancelClick() },
    onLocationLockClick = { viewModel.onLocationLockClick() },
    onStopDownloading = { viewModel.stopDownloading() },
  )
}

/**
 * Stateless composable for the Offline Area Selector screen.
 *
 * @param uiState Current UI state of the selector screen.
 * @param locationLockIconType The current icon type for the location lock button.
 * @param onDownloadClick Callback when download button is clicked.
 * @param onCancelClick Callback when cancel button is clicked.
 * @param onLocationLockClick Callback when location lock button is clicked.
 * @param onStopDownloading Callback to cancel and dismiss the active download dialog.
 */
@VisibleForTesting
@Composable
fun OfflineAreaSelectorScreen(
  uiState: OfflineAreaSelectorState,
  locationLockIconType: MapFloatingActionButtonType,
  onDownloadClick: () -> Unit,
  onCancelClick: () -> Unit,
  onLocationLockClick: () -> Unit,
  onStopDownloading: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
      Toolbar(
        stringRes = R.string.offline_area_selector_title,
        showNavigationIcon = false,
        titleCentered = true,
        iconClick = {},
      )

      OfflineAreaViewportOverlay(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        bottomTextState = uiState.bottomTextState,
        locationLockIconType = locationLockIconType,
        onLocationLockClick = onLocationLockClick,
      )

      OfflineAreaSelectorButtons(
        isDownloadEnabled = uiState.isDownloadButtonEnabled,
        onCancelClick = onCancelClick,
        onDownloadClick = onDownloadClick,
      )
    }

    val downloadState = uiState.downloadState
    if (downloadState is OfflineAreaSelectorState.DownloadState.InProgress) {
      DownloadProgressDialog(progress = downloadState.progress, onDismiss = onStopDownloading)
    }
  }
}

@Composable
private fun OfflineAreaViewportOverlay(
  bottomTextState: OfflineAreaSelectorState.BottomTextState?,
  locationLockIconType: MapFloatingActionButtonType,
  onLocationLockClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val maskColor = colorResource(R.color.blackOverlay).copy(alpha = 0.4f)
  Box(modifier = modifier) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top mask
      Box(modifier = Modifier.fillMaxWidth().height(24.dp).background(maskColor))

      // Center row: Left mask, Viewport Outline, Right mask
      Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxHeight().width(24.dp).background(maskColor))

        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxHeight()
              .border(3.dp, colorResource(R.color.md_theme_inversePrimary))
        ) {
          MapFloatingActionButton(
            modifier = Modifier.align(Alignment.BottomEnd),
            type = locationLockIconType,
            onClick = onLocationLockClick,
          )
        }

        Box(modifier = Modifier.fillMaxHeight().width(24.dp).background(maskColor))
      }

      // Bottom mask containing bottom text
      Box(
        modifier = Modifier.fillMaxWidth().height(80.dp).background(maskColor),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = getBottomTextMessage(bottomTextState),
          color = colorResource(R.color.textOverMap),
          fontSize = 14.sp,
          textAlign = TextAlign.Center,
          modifier =
            Modifier.padding(horizontal = 64.dp)
              .testTag(OFFLINE_AREA_SELECTOR_BOTTOM_TEXT_TEST_TAG),
        )
      }
    }
  }
}

@Composable
private fun OfflineAreaSelectorButtons(
  isDownloadEnabled: Boolean,
  onCancelClick: () -> Unit,
  onDownloadClick: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
          .padding(horizontal = 16.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      OutlinedButton(
        modifier = Modifier.weight(1f).testTag(OFFLINE_AREA_SELECTOR_CANCEL_BUTTON_TEST_TAG),
        onClick = onCancelClick,
      ) {
        Text(text = stringResource(R.string.offline_area_select_cancel_button))
      }

      Button(
        modifier = Modifier.weight(1f).testTag(OFFLINE_AREA_SELECTOR_DOWNLOAD_BUTTON_TEST_TAG),
        onClick = onDownloadClick,
        enabled = isDownloadEnabled,
      ) {
        Text(text = stringResource(R.string.offline_area_selector_download))
      }
    }
  }
}

@Composable
private fun getBottomTextMessage(state: OfflineAreaSelectorState.BottomTextState?): String =
  when (state) {
    is OfflineAreaSelectorState.BottomTextState.AreaSize ->
      stringResource(R.string.selected_offline_area_size, state.size)
    OfflineAreaSelectorState.BottomTextState.AreaTooLarge ->
      stringResource(R.string.selected_offline_area_too_large)
    OfflineAreaSelectorState.BottomTextState.Loading ->
      stringResource(
        R.string.selected_offline_area_size,
        stringResource(R.string.offline_area_size_loading_symbol),
      )
    OfflineAreaSelectorState.BottomTextState.NetworkError ->
      stringResource(R.string.connect_to_download_message)
    OfflineAreaSelectorState.BottomTextState.NoImageryAvailable ->
      stringResource(R.string.no_imagery_available_for_area)
    null -> ""
  }

@VisibleForTesting
@Composable
fun DownloadProgressDialog(
  progress: Float,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AlertDialog(
    onDismissRequest = {},
    modifier = modifier,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    title = {
      Text(
        stringResource(
          R.string.offline_map_imagery_download_progress_dialog_title,
          (progress * 100).toInt(),
        ),
        color = MaterialTheme.colorScheme.onSurface,
      )
    },
    text = {
      Column {
        val animatedProgress by
          animateFloatAsState(targetValue = progress, animationSpec = tween(durationMillis = 300))

        LinearProgressIndicator(
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).testTag("progressBar"),
          progress = { animatedProgress },
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
          stringResource(R.string.offline_map_imagery_download_progress_dialog_message),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    dismissButton = {
      TextButton(
        modifier = Modifier.testTag(DOWNLOAD_PROGRESS_DIALOG_CANCEL_BUTTON_TEST_TAG),
        onClick = { onDismiss() },
      ) {
        Text(text = stringResource(R.string.cancel))
      }
    },
    confirmButton = {},
  )
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun OfflineAreaSelectorScreenDefaultPreview() {
  AppTheme {
    OfflineAreaSelectorScreen(
      uiState = OfflineAreaSelectorState(),
      locationLockIconType = MapFloatingActionButtonType.LocationNotLocked,
      onDownloadClick = {},
      onCancelClick = {},
      onLocationLockClick = {},
      onStopDownloading = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun OfflineAreaSelectorScreenDownloadablePreview() {
  AppTheme {
    OfflineAreaSelectorScreen(
      uiState =
        OfflineAreaSelectorState(
          bottomTextState = OfflineAreaSelectorState.BottomTextState.AreaSize("5.0")
        ),
      locationLockIconType = MapFloatingActionButtonType.LocationLocked(),
      onDownloadClick = {},
      onCancelClick = {},
      onLocationLockClick = {},
      onStopDownloading = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun OfflineAreaSelectorScreenDownloadingPreview() {
  AppTheme {
    OfflineAreaSelectorScreen(
      uiState =
        OfflineAreaSelectorState(
          bottomTextState = OfflineAreaSelectorState.BottomTextState.AreaSize("5.0"),
          downloadState = OfflineAreaSelectorState.DownloadState.InProgress(0.45f),
        ),
      locationLockIconType = MapFloatingActionButtonType.LocationLocked(),
      onDownloadClick = {},
      onCancelClick = {},
      onLocationLockClick = {},
      onStopDownloading = {},
    )
  }
}
