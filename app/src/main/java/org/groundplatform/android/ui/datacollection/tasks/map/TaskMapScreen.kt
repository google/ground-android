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
package org.groundplatform.android.ui.datacollection.tasks.map

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.components.MapFloatingActionButton
import org.groundplatform.android.ui.components.MapFloatingActionButtonType
import org.groundplatform.android.ui.components.RecenterButton
import org.groundplatform.android.ui.datacollection.tasks.map.components.LocationInfo
import org.groundplatform.android.ui.datacollection.tasks.map.components.LocationInfoCard
import org.groundplatform.ui.theme.AppTheme

@VisibleForTesting const val TASK_MAP_CENTER_MARKER_TEST_TAG = "task_map_center_marker"

/**
 * Screen overlay for map-based tasks, including center crosshairs, map type button, recenter
 * button, location lock button, and location info card.
 */
@Composable
fun TaskMapScreen(
  locationLockButtonType: MapFloatingActionButtonType,
  shouldShowRecenter: Boolean,
  isCenterMarkerVisible: Boolean,
  locationInfo: LocationInfo?,
  onMapTypeClicked: () -> Unit,
  onLocationLockClicked: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize()) {
    if (isCenterMarkerVisible) {
      Image(
        painter = painterResource(R.drawable.ic_plus_sign),
        contentDescription = null,
        modifier =
          Modifier.align(Alignment.Center).scale(0.5f).testTag(TASK_MAP_CENTER_MARKER_TEST_TAG),
      )
    }

    MapFloatingActionButton(
      modifier = Modifier.align(Alignment.TopEnd),
      type = MapFloatingActionButtonType.MapType,
      onClick = onMapTypeClicked,
    )

    Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
      if (shouldShowRecenter) {
        RecenterButton(
          modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
          onClick = onLocationLockClicked,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Bottom,
      ) {
        if (locationInfo != null) {
          LocationInfoCard(
            locationInfo = locationInfo,
            modifier = Modifier.weight(1f).padding(bottom = 16.dp, end = 8.dp),
          )
        } else {
          Spacer(modifier = Modifier.weight(1f))
        }

        MapFloatingActionButton(type = locationLockButtonType, onClick = onLocationLockClicked)
      }
    }
  }
}

@Preview
@Composable
@ExcludeFromJacocoGeneratedReport
private fun TaskMapScreenPreview() {
  AppTheme {
    TaskMapScreen(
      locationLockButtonType = MapFloatingActionButtonType.LocationNotLocked,
      shouldShowRecenter = true,
      isCenterMarkerVisible = true,
      locationInfo =
        LocationInfo(
          titleRes = R.string.current_location,
          locationText = "29º58’15” N  114º36’17”W",
          accuracyText = "3m",
          isAccuracyGood = true,
        ),
      onMapTypeClicked = {},
      onLocationLockClicked = {},
    )
  }
}
