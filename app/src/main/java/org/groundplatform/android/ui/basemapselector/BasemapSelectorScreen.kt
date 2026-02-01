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
package org.groundplatform.android.ui.basemapselector

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.model.map.MapType
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.theme.AppTheme

/**
 * Screen containing a list of [MapType] for updating basemap layer.
 *
 * This is displayed within a ModalBottomSheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasemapSelectorScreen(
  mapTypes: List<MapType>,
  onDismissRequest: () -> Unit,
  viewModel: BasemapSelectorViewModel = hiltViewModel(),
) {
  val currentMapType by viewModel.currentMapType.collectAsStateWithLifecycle()
  val isOfflineImageryEnabled by viewModel.isOfflineImageryEnabled.collectAsStateWithLifecycle()
  val sheetState = rememberModalBottomSheetState()
  val scope = rememberCoroutineScope()

  ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
    BasemapSelectorContent(
      mapTypes = mapTypes,
      selectedMapType = currentMapType,
      isOfflineImageryEnabled = isOfflineImageryEnabled,
      onMapTypeSelected = { mapType ->
        scope
          .launch { sheetState.hide() }
          .invokeOnCompletion {
            if (!sheetState.isVisible) {
              viewModel.updateMapType(mapType)
              onDismissRequest()
            }
          }
      },
      onOfflineImageryStateChanged = { viewModel.setOfflineImageryEnabled(it) },
    )
  }
}

/**
 * Content of the Map Type Selector, including the list of map types and the offline imagery switch.
 */
@Composable
private fun BasemapSelectorContent(
  mapTypes: List<MapType>,
  selectedMapType: MapType,
  isOfflineImageryEnabled: Boolean,
  onMapTypeSelected: (MapType) -> Unit,
  onOfflineImageryStateChanged: (Boolean) -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .padding(start = 24.dp, end = 24.dp, bottom = 64.dp)
        .verticalScroll(rememberScrollState())
  ) {
    Text(text = stringResource(R.string.layers), style = MaterialTheme.typography.titleLarge)

    BasemapSwitcher(
      mapTypes = mapTypes,
      selectedMapType = selectedMapType,
      onMapTypeSelected = onMapTypeSelected,
    )

    HorizontalDivider(modifier = Modifier.fillMaxWidth())

    OfflineImageryToggle(
      enabled = isOfflineImageryEnabled,
      onEnabledChange = onOfflineImageryStateChanged,
    )
  }
}

@Composable
private fun BasemapSwitcher(
  mapTypes: List<MapType>,
  selectedMapType: MapType,
  onMapTypeSelected: (MapType) -> Unit,
) {
  Text(
    text = stringResource(R.string.base_map),
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onSurface,
    modifier = Modifier.padding(top = 27.dp),
  )

  LazyRow(
    modifier = Modifier.fillMaxWidth().padding(top = 11.dp, bottom = 14.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    items(mapTypes) { item ->
      BasemapTypeItem(
        mapType = item,
        isSelected = item == selectedMapType,
        onClick = { onMapTypeSelected(item) },
      )
    }
  }
}

@Composable
private fun OfflineImageryToggle(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 38.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = stringResource(R.string.offline_map_imagery),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = stringResource(R.string.offline_map_imagery_pref_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 4.dp),
      )
    }
    Switch(
      checked = enabled,
      onCheckedChange = onEnabledChange,
      modifier = Modifier.padding(start = 16.dp),
    )
  }
}

/** Single item in the map type list, displaying the map image and label. */
@Composable
private fun BasemapTypeItem(mapType: MapType, isSelected: Boolean, onClick: () -> Unit) {
  val borderColor =
    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
  val borderWidth = if (isSelected) 2.dp else 0.dp

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.clickable { onClick() },
  ) {
    Image(
      painter = painterResource(mapType.drawableResId()),
      contentDescription = stringResource(mapType.labelResId()),
      contentScale = ContentScale.Crop,
      modifier =
        Modifier.size(96.dp)
          .clip(RoundedCornerShape(16.dp))
          .border(borderWidth, borderColor, RoundedCornerShape(16.dp)),
    )
    Text(
      text = stringResource(mapType.labelResId()),
      style = MaterialTheme.typography.labelMedium,
      color =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(top = 10.dp),
    )
  }
}

/** Returns the drawable resource ID for the map type image. */
@DrawableRes
private fun MapType.drawableResId(): Int =
  when (this) {
    MapType.ROAD -> R.drawable.map_type_roadmap
    MapType.TERRAIN -> R.drawable.map_type_terrain
    MapType.SATELLITE -> R.drawable.map_type_satellite
  }

/** Returns the string resource ID for the map type label. */
@StringRes
private fun MapType.labelResId(): Int =
  when (this) {
    MapType.ROAD -> R.string.road_map
    MapType.TERRAIN -> R.string.terrain
    MapType.SATELLITE -> R.string.satellite
  }

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
fun BasemapSelectorScreenPreview() {
  AppTheme {
    BasemapSelectorContent(
      mapTypes = listOf(MapType.ROAD, MapType.TERRAIN, MapType.SATELLITE),
      selectedMapType = MapType.TERRAIN,
      isOfflineImageryEnabled = false,
      onMapTypeSelected = {},
      onOfflineImageryStateChanged = {},
    )
  }
}
