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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import org.groundplatform.android.R
import org.groundplatform.android.model.map.MapType
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.theme.AppTheme

@Composable
fun MapTypeScreen(onMapTypeSelected: () -> Unit, viewModel: MapTypeViewModel = hiltViewModel()) {
  val mapType by viewModel.mapTypeFlow.collectAsStateWithLifecycle()
  val offlineImageryEnabled by viewModel.offlineImageryEnabledFlow.collectAsStateWithLifecycle()

  MapTypeContent(
    mapType = mapType,
    offlineImageryEnabled = offlineImageryEnabled,
    onMapTypeSelected = {
      viewModel.mapType = it
      onMapTypeSelected()
    },
    onOfflineImageryEnabledChange = { viewModel.updateOfflineImageryPreference(it) },
  )
}

@Composable
private fun MapTypeContent(
  mapType: MapType,
  offlineImageryEnabled: Boolean,
  onMapTypeSelected: (MapType) -> Unit,
  onOfflineImageryEnabledChange: (Boolean) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 64.dp)) {
    // Drag handle
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
      horizontalArrangement = Arrangement.Center,
    ) {
      Spacer(
        modifier =
          Modifier.width(32.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .border(
              width = 4.dp,
              color = MaterialTheme.colorScheme.outlineVariant,
              shape = RoundedCornerShape(2.dp),
            )
      )
    }

    Text(
      text = stringResource(R.string.layers),
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier.padding(top = 16.dp),
    )

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
      items(MapType.entries) { item ->
        MapTypeItem(
          mapType = item,
          isSelected = item == mapType,
          onClick = { onMapTypeSelected(item) },
        )
      }
    }

    HorizontalDivider(modifier = Modifier.fillMaxWidth())

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
        checked = offlineImageryEnabled,
        onCheckedChange = onOfflineImageryEnabledChange,
        modifier = Modifier.padding(start = 16.dp),
      )
    }
  }
}

@Composable
private fun MapTypeItem(mapType: MapType, isSelected: Boolean, onClick: () -> Unit) {
  val borderColor =
    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
  val borderWidth = if (isSelected) 2.dp else 0.dp

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.clickable { onClick() },
  ) {
    Image(
      painter = painterResource(mapType.imageId()),
      contentDescription = stringResource(mapType.labelId()),
      contentScale = ContentScale.Crop,
      modifier =
        Modifier.size(96.dp)
          .clip(RoundedCornerShape(16.dp))
          .border(borderWidth, borderColor, RoundedCornerShape(16.dp)),
    )
    Text(
      text = stringResource(mapType.labelId()),
      style = MaterialTheme.typography.labelMedium,
      color =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(top = 10.dp),
    )
  }
}

@DrawableRes
private fun MapType.imageId(): Int =
  when (this) {
    MapType.ROAD -> R.drawable.map_type_roadmap
    MapType.TERRAIN -> R.drawable.map_type_terrain
    MapType.SATELLITE -> R.drawable.map_type_satellite
  }

@StringRes
private fun MapType.labelId(): Int =
  when (this) {
    MapType.ROAD -> R.string.road_map
    MapType.TERRAIN -> R.string.terrain
    MapType.SATELLITE -> R.string.satellite
  }

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
fun MapTypeScreenPreview() {
  AppTheme {
    MapTypeContent(
      mapType = MapType.TERRAIN,
      offlineImageryEnabled = false,
      onMapTypeSelected = {},
      onOfflineImageryEnabledChange = {},
    )
  }
}
