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

package org.groundplatform.android.ui.offlineareas

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.components.Toolbar
import org.groundplatform.ui.theme.AppTheme

const val OFFLINE_AREAS_LIST_TEST_TAG = "offline area list"
const val OFFLINE_AREAS_SELECT_FAB_TEST_TAG = "offline_areas_select_fab"
const val OFFLINE_AREAS_LIST_TITLE_TEST_TAG = "offline_areas_list_title"
const val OFFLINE_AREAS_LIST_TIP_TEST_TAG = "offline_areas_list_tip"
const val OFFLINE_AREAS_NO_AREAS_MESSAGE_TEST_TAG = "offline_areas_no_areas_message"
const val OFFLINE_AREAS_LOADING_SPINNER_TEST_TAG = "offline_areas_loading_spinner"

/**
 * Stateful entry point for the Offline Areas list screen.
 *
 * @param viewModel The ViewModel providing UI state and actions.
 * @param onAreaClick Callback when an offline area item in the list is clicked.
 * @param onNavigateUp Callback when the back navigation icon is clicked.
 */
@Composable
fun OfflineAreasScreen(
  viewModel: OfflineAreasViewModel,
  onAreaClick: (String) -> Unit,
  onNavigateUp: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  OfflineAreasScreen(
    uiState = uiState,
    onAreaClick = onAreaClick,
    onSelectAreaClick = { viewModel.showOfflineAreaSelector() },
    onNavigateUp = onNavigateUp,
  )
}

/**
 * Stateless composable for the Offline Areas list screen.
 *
 * @param uiState Current UI state of the offline areas list screen.
 * @param onAreaClick Callback when an offline area item in the list is clicked.
 * @param onSelectAreaClick Callback when the select area FAB is clicked.
 * @param onNavigateUp Callback when the back navigation icon is clicked.
 * @param modifier Modifier for the root container.
 */
@VisibleForTesting
@Composable
fun OfflineAreasScreen(
  uiState: OfflineAreasState,
  onAreaClick: (String) -> Unit,
  onSelectAreaClick: () -> Unit,
  onNavigateUp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      Toolbar(
        stringRes = R.string.offline_map_imagery,
        showNavigationIcon = true,
        iconClick = onNavigateUp,
      )
    },
    floatingActionButton = { OfflineAreasSelectFab(onSelectAreaClick) },
    floatingActionButtonPosition = FabPosition.Center,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      when {
        uiState.isLoading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
              modifier = Modifier.testTag(OFFLINE_AREAS_LOADING_SPINNER_TEST_TAG)
            )
          }
        }

        uiState.isEmpty -> {
          OfflineAreasEmptyView()
        }

        else -> {
          OfflineAreasLoadedView(uiState.offlineAreas, onAreaClick)
        }
      }
    }
  }
}

@Composable
private fun OfflineAreasSelectFab(onClick: () -> Unit) {
  ExtendedFloatingActionButton(
    onClick = onClick,
    icon = {
      Icon(
        painter = painterResource(R.drawable.ic_maps_ar),
        contentDescription = stringResource(R.string.offline_area_selector_prompt),
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
      )
    },
    text = { Text(text = stringResource(R.string.offline_area_selector_select)) },
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    modifier = Modifier.testTag(OFFLINE_AREAS_SELECT_FAB_TEST_TAG),
  )
}

@Composable
private fun OfflineAreasEmptyView() {
  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Image(
      painter = painterResource(R.drawable.offline_map_imagery),
      contentDescription = stringResource(R.string.offline_map_imagery_no_areas_downloaded_image),
      modifier = Modifier.size(200.dp),
    )
    Text(
      text = stringResource(R.string.no_basemaps_downloaded),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(top = 16.dp).testTag(OFFLINE_AREAS_NO_AREAS_MESSAGE_TEST_TAG),
    )
  }
}

@Composable
private fun OfflineAreasLoadedView(
  areas: List<OfflineAreaDetails>,
  onAreaClick: (String) -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier =
        Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 24.dp, bottom = 8.dp)
    ) {
      Text(
        text = stringResource(R.string.offline_downloaded_areas),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag(OFFLINE_AREAS_LIST_TITLE_TEST_TAG),
      )
      Text(
        text = stringResource(R.string.offline_area_list_tip),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag(OFFLINE_AREAS_LIST_TIP_TEST_TAG),
      )
    }

    LazyColumn(modifier = Modifier.fillMaxSize().testTag(OFFLINE_AREAS_LIST_TEST_TAG)) {
      items(items = areas, key = { it.id }) { area ->
        OfflineAreaListItem(offlineAreaDetails = area, itemClicked = onAreaClick)
      }
    }
  }
}

@Composable
private fun OfflineAreaListItem(
  offlineAreaDetails: OfflineAreaDetails,
  modifier: Modifier = Modifier,
  itemClicked: (areaId: String) -> Unit = {},
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(start = 16.dp, top = 4.dp, end = 24.dp, bottom = 4.dp)
        .clickable { itemClicked(offlineAreaDetails.id) },
    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = ImageVector.vectorResource(id = R.drawable.ic_offline_pin),
      contentDescription = stringResource(id = R.string.offline_area_list_item_icon),
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(24.dp),
    )

    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
      horizontalAlignment = Alignment.Start,
    ) {
      Text(text = offlineAreaDetails.name, color = MaterialTheme.colorScheme.onSurface)

      Text(
        text =
          stringResource(
            id = R.string.offline_area_list_item_size_on_disk_mb,
            offlineAreaDetails.sizeOnDisk,
          ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@ExcludeFromJacocoGeneratedReport
@Preview(showBackground = true)
@Composable
private fun OfflineAreasScreenEmptyPreview() {
  AppTheme {
    OfflineAreasScreen(
      uiState = OfflineAreasState(offlineAreas = emptyList(), isLoading = false),
      onAreaClick = {},
      onSelectAreaClick = {},
      onNavigateUp = {},
    )
  }
}

@ExcludeFromJacocoGeneratedReport
@Preview(showBackground = true)
@Composable
private fun OfflineAreasScreenLoadedPreview() {
  AppTheme {
    OfflineAreasScreen(
      uiState =
        OfflineAreasState(
          offlineAreas =
            listOf(
              OfflineAreaDetails("id_1", "Downtown", "1.2 MB"),
              OfflineAreaDetails("id_2", "Forest Reserve", "4.5 MB"),
            ),
          isLoading = false,
        ),
      onAreaClick = {},
      onSelectAreaClick = {},
      onNavigateUp = {},
    )
  }
}
