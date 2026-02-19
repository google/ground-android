/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.ui.home.mapcontainer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.Style
import org.groundplatform.android.ui.components.MapFloatingActionButton
import org.groundplatform.android.ui.components.MapFloatingActionButtonType
import org.groundplatform.android.ui.components.RecenterButton
import org.groundplatform.android.ui.home.mapcontainer.jobs.AdHocDataCollectionButtonData
import org.groundplatform.android.ui.home.mapcontainer.jobs.JobMapComponent
import org.groundplatform.android.ui.home.mapcontainer.jobs.JobMapComponentAction
import org.groundplatform.android.ui.home.mapcontainer.jobs.JobMapComponentState
import org.groundplatform.android.ui.theme.AppTheme

@Composable
fun HomeScreenMapContainerScreen(
  modifier: Modifier = Modifier,
  locationLockButtonType: MapFloatingActionButtonType,
  shouldShowMapActions: Boolean,
  shouldShowRecenter: Boolean,
  jobComponentState: JobMapComponentState,
  onBaseMapAction: (BaseMapAction) -> Unit,
  onJobComponentAction: (JobMapComponentAction) -> Unit,
) {
  Box(modifier = modifier.fillMaxSize()) {
    if (shouldShowMapActions) {
      MapFloatingActionButton(
        modifier =
          Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .align(Alignment.TopStart),
        type = MapFloatingActionButtonType.OpenNavDrawer,
        onClick = { onBaseMapAction(BaseMapAction.OnOpenNavDrawerClicked) },
      )

      MapFloatingActionButton(
        modifier =
          Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .align(Alignment.TopEnd),
        type = MapFloatingActionButtonType.MapType,
        onClick = { onBaseMapAction(BaseMapAction.OnMapTypeClicked) },
      )
    }

    Column(
      modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      if (shouldShowMapActions) {
        LocationLockComponent(
          shouldShowRecenter = shouldShowRecenter,
          locationLockButtonType = locationLockButtonType,
          onAction = onBaseMapAction,
        )
      }

      JobMapComponent(state = jobComponentState, onAction = onJobComponentAction)
    }
  }
}

@Composable
private fun LocationLockComponent(
  modifier: Modifier = Modifier,
  shouldShowRecenter: Boolean,
  locationLockButtonType: MapFloatingActionButtonType,
  onAction: (BaseMapAction) -> Unit,
) {
  Row(
    modifier =
      modifier
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (shouldShowRecenter)
      RecenterButton(
        modifier = Modifier.padding(start = 16.dp),
        onClick = { onAction(BaseMapAction.OnLocationLockClicked) },
      )

    Spacer(modifier = Modifier.weight(1f))

    MapFloatingActionButton(
      type = locationLockButtonType,
      onClick = { onAction(BaseMapAction.OnLocationLockClicked) },
    )
  }
}

sealed interface BaseMapAction {
  data object OnMapTypeClicked : BaseMapAction

  data object OnLocationLockClicked : BaseMapAction

  data object OnOpenNavDrawerClicked : BaseMapAction
}

@Preview(showSystemUi = true)
@Composable
private fun HomeScreenMapContainerScreenPreview() {
  AppTheme {
    HomeScreenMapContainerScreen(
      locationLockButtonType = MapFloatingActionButtonType.LocationNotLocked,
      jobComponentState =
        JobMapComponentState(
          selectedLoi = null,
          adHocDataCollectionButtonData =
            listOf(
              AdHocDataCollectionButtonData(
                canCollectData = true,
                job =
                  Job(
                    id = "1",
                    style = Style(color = "#4169E1"),
                    name = "job 1",
                    tasks = emptyMap(),
                  ),
              )
            ),
        ),
      shouldShowMapActions = true,
      shouldShowRecenter = true,
      onBaseMapAction = {},
      onJobComponentAction = {},
    )
  }
}
