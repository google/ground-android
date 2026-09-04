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

package org.groundplatform.android.ui.offlineareas.viewer

import androidx.activity.compose.BackHandler
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.components.Toolbar
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.imagery.OfflineArea
import org.groundplatform.domain.model.map.Bounds
import org.groundplatform.ui.theme.AppTheme
import org.groundplatform.ui.theme.sizes

const val OFFLINE_AREA_VIEWER_NAME_TEST_TAG = "offline_area_viewer_name"
const val OFFLINE_AREA_VIEWER_SIZE_TEST_TAG = "offline_area_viewer_size"
const val OFFLINE_AREA_VIEWER_REMOVE_BUTTON_TEST_TAG = "offline_area_viewer_remove_button"
const val OFFLINE_AREA_VIEWER_PROGRESS_OVERLAY_TEST_TAG = "offline_area_viewer_progress_overlay"

@Composable
fun OfflineAreaViewerScreen(viewModel: OfflineAreaViewerViewModel, onNavigateUp: () -> Unit) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  OfflineAreaViewerScreen(
    uiState = uiState,
    onRemoveClick = { viewModel.onRemoveButtonClick() },
    onNavigateUp = onNavigateUp,
  )
}

@VisibleForTesting
@Composable
fun OfflineAreaViewerScreen(
  uiState: OfflineAreaViewerState,
  onRemoveClick: () -> Unit,
  onNavigateUp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(enabled = uiState.isProgressOverlayVisible) {}

  Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
      Toolbar(
        stringRes = R.string.offline_area_viewer_title,
        showNavigationIcon = true,
        titleCentered = true,
        iconClick = { if (!uiState.isProgressOverlayVisible) onNavigateUp() },
      )

      Spacer(modifier = Modifier.weight(1f))

      OfflineAreaDetailsCard(
        areaName = uiState.areaName,
        areaSize = uiState.areaSize,
        isRemoveButtonEnabled = uiState.isRemoveButtonEnabled,
        onRemoveClick = onRemoveClick,
      )
    }

    if (uiState.isProgressOverlayVisible) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(colorResource(R.color.blackOverlay).copy(alpha = 0.5f))
            .clickable(
              enabled = true,
              indication = null,
              interactionSource = remember { MutableInteractionSource() },
              onClick = {},
            )
            .testTag(OFFLINE_AREA_VIEWER_PROGRESS_OVERLAY_TEST_TAG),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(MaterialTheme.sizes.offlineAreaViewerProgressIndicatorSize),
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
private fun OfflineAreaDetailsCard(
  areaName: String,
  areaSize: String?,
  isRemoveButtonEnabled: Boolean,
  onRemoveClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
          .padding(horizontal = 24.dp)
          .padding(top = 40.dp, bottom = 48.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = areaName,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().testTag(OFFLINE_AREA_VIEWER_NAME_TEST_TAG),
      )

      if (areaSize != null) {
        Text(
          text = stringResource(R.string.offline_area_size_on_disk_mb, areaSize),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth().testTag(OFFLINE_AREA_VIEWER_SIZE_TEST_TAG),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = onRemoveClick,
        enabled = isRemoveButtonEnabled,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
          ),
        modifier = Modifier.testTag(OFFLINE_AREA_VIEWER_REMOVE_BUTTON_TEST_TAG),
      ) {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = null,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.offline_area_viewer_remove_button))
      }
    }
  }
}

@ExcludeFromJacocoGeneratedReport
@Preview(showBackground = true)
@Composable
private fun OfflineAreaViewerScreenPreview() {
  AppTheme {
    OfflineAreaViewerScreen(
      uiState =
        OfflineAreaViewerState(
          area =
            OfflineArea(
              id = "id_1",
              name = "Test Area",
              bounds = Bounds(Coordinates(0.0, 0.0), Coordinates(1.0, 1.0)),
              state = OfflineArea.State.DOWNLOADED,
              zoomRange = 0..10,
            ),
          areaName = "Test Area",
          areaSize = "1.5",
          isProgressOverlayVisible = false,
        ),
      onRemoveClick = {},
      onNavigateUp = {},
    )
  }
}

@ExcludeFromJacocoGeneratedReport
@Preview(showBackground = true)
@Composable
private fun OfflineAreaViewerScreenLoadingPreview() {
  AppTheme {
    OfflineAreaViewerScreen(
      uiState = OfflineAreaViewerState(isProgressOverlayVisible = true),
      onRemoveClick = {},
      onNavigateUp = {},
    )
  }
}
