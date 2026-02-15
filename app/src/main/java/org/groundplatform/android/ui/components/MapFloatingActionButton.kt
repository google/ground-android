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
package org.groundplatform.android.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.groundplatform.android.R
import org.groundplatform.android.ui.theme.AppTheme

const val OPEN_NAV_DRAWER_TEST_TAG = "open_nav_drawer"
const val CHOOSE_MAP_TYPE_TEST_TAG = "choose_map_type"
const val LOCATION_LOCKED_TEST_TAG = "location_locked"
const val LOCATION_NOT_LOCKED_TEST_TAG = "location_not_locked"

@Composable
fun MapFloatingActionButton(
  modifier: Modifier = Modifier,
  type: MapFloatingActionButtonType,
  onClick: () -> Unit,
) {
  FloatingActionButton(
    modifier = modifier.testTag(type.testTag).padding(16.dp),
    containerColor = MaterialTheme.colorScheme.secondaryContainer,
    onClick = onClick,
  ) {
    Icon(
      imageVector = ImageVector.vectorResource(type.iconRes),
      contentDescription = null,
      tint = colorResource(type.iconTintRes),
    )
  }
}

sealed class MapFloatingActionButtonType(
  val iconRes: Int,
  open val iconTintRes: Int = R.color.md_theme_onSurface,
  val testTag: String,
) {
  data object OpenNavDrawer :
    MapFloatingActionButtonType(
      iconRes = R.drawable.baseline_menu_24,
      testTag = OPEN_NAV_DRAWER_TEST_TAG,
    )

  data object MapType :
    MapFloatingActionButtonType(iconRes = R.drawable.map_layers, testTag = CHOOSE_MAP_TYPE_TEST_TAG)

  // TODO: Consider adding another icon for representing "GPS disabled" state.
  // Issue URL: https://github.com/google/ground-android/issues/1789
  data class LocationLocked(override val iconTintRes: Int = R.color.md_theme_primary) :
    MapFloatingActionButtonType(
      iconRes = R.drawable.ic_gps_lock,
      iconTintRes = iconTintRes,
      testTag = LOCATION_LOCKED_TEST_TAG,
    )

  data object LocationNotLocked :
    MapFloatingActionButtonType(
      iconRes = R.drawable.ic_gps_lock_not_fixed,
      testTag = LOCATION_NOT_LOCKED_TEST_TAG,
    )
}

@Preview
@Composable
private fun MapFloatingActionButtonPreview() {
  AppTheme {
    MapFloatingActionButton(type = MapFloatingActionButtonType.LocationLocked(), onClick = {})
  }
}
