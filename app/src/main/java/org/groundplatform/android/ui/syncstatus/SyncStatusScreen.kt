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

package org.groundplatform.android.ui.syncstatus

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.components.Toolbar
import org.groundplatform.domain.model.mutation.Mutation
import org.groundplatform.ui.theme.AppTheme

const val SYNC_STATUS_LIST_TEST_TAG = "sync list"

/**
 * Stateful entry point for the Sync Status screen.
 *
 * @param viewModel The ViewModel providing UI state.
 * @param onNavigateUp Callback when the back navigation icon is clicked.
 */
@Composable
fun SyncStatusScreen(
  viewModel: SyncStatusViewModel,
  onNavigateUp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  SyncStatusScreen(uiState = uiState, onNavigateUp = onNavigateUp, modifier = modifier)
}

/**
 * Stateless composable for the Sync Status screen.
 *
 * @param uiState Current UI state of the sync status screen.
 * @param onNavigateUp Callback when the back navigation icon is clicked.
 * @param modifier Modifier for the root container.
 */
@VisibleForTesting
@Composable
fun SyncStatusScreen(
  uiState: SyncStatusState,
  onNavigateUp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      Toolbar(
        stringRes = R.string.data_sync_status,
        showNavigationIcon = true,
        titleCentered = true,
        iconClick = onNavigateUp,
      )
    },
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding).testTag(SYNC_STATUS_LIST_TEST_TAG)
    ) {
      items(items = uiState.items) { item ->
        SyncListItem(detail = item, modifier = Modifier.semantics { testTag = "item ${item.user}" })
      }
    }
  }
}

@ExcludeFromJacocoGeneratedReport
@Preview(showBackground = true)
@Composable
private fun SyncStatusScreenEmptyPreview() {
  AppTheme { SyncStatusScreen(uiState = SyncStatusState(), onNavigateUp = {}) }
}

@ExcludeFromJacocoGeneratedReport
@Preview(showBackground = true)
@Composable
private fun SyncStatusScreenLoadedPreview() {
  AppTheme {
    SyncStatusScreen(
      uiState =
        SyncStatusState(
          items =
            listOf(
              SyncStatusDetail(
                user = "Jane Doe",
                status = Mutation.SyncStatus.PENDING,
                timestamp = 1700000000000L,
                label = "Map the farms",
                subtitle = "IDX21311",
                description = "Lacuna Fund Cocoa Mapping",
              ),
              SyncStatusDetail(
                user = "John Smith",
                status = Mutation.SyncStatus.IN_PROGRESS,
                timestamp = 1700000100000L,
                label = "Forest Survey",
                subtitle = "Site A",
                description = "Tree canopy density",
              ),
            )
        ),
      onNavigateUp = {},
    )
  }
}
