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
package org.groundplatform.android.ui.datacollection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import kotlin.time.Clock
import kotlinx.serialization.json.JsonObject
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.locationofinterest.LoiReport
import org.groundplatform.ui.theme.AppTheme

private const val PAGER_CONTENT_TEXT = "Pager Content Area"

@Preview(showBackground = true, showSystemUi = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun DataCollectionContentLoadingPreview() {
  AppTheme {
    DataCollectionContent(uiState = DataCollectionUiState.Loading, onCloseClicked = {}) {
      Box(modifier = Modifier.fillMaxSize().background(Color.LightGray)) {
        Text(text = PAGER_CONTENT_TEXT, modifier = Modifier.align(Alignment.Center))
      }
    }
  }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun DataCollectionContentErrorPreview() {
  AppTheme {
    DataCollectionContent(
      uiState =
        DataCollectionUiState.Error(
          code = DataCollectionErrorCode.NO_VALID_TASKS,
          cause = Error("Some error"),
        ),
      onCloseClicked = {},
    ) {
      Box(modifier = Modifier.fillMaxSize().background(Color.LightGray)) {
        Text(text = PAGER_CONTENT_TEXT, modifier = Modifier.align(Alignment.Center))
      }
    }
  }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun DataCollectionContentPreview() {
  AppTheme {
    DataCollectionContent(
      uiState =
        DataCollectionUiState.Ready(
          surveyId = "survey1",
          job = Job(id = "job1", name = "Test Job"),
          loiName = "Test LOI",
          tasks = emptyList(),
          isAddLoiFlow = false,
          currentTaskId = "task1",
          position = TaskPosition(0, 1, 3),
        ),
      onCloseClicked = {},
    ) {
      Box(modifier = Modifier.fillMaxSize().background(Color.LightGray)) {
        Text(text = PAGER_CONTENT_TEXT, modifier = Modifier.align(Alignment.Center))
      }
    }
  }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun DataCollectionContentCompletePreview() {
  AppTheme {
    DataCollectionContent(
      uiState =
        DataCollectionUiState.TaskSubmitted(
          loiReport =
            LoiReport(
              loiName = "Point A",
              geoJson = JsonObject(mapOf()),
              submissionDetails = null
            )
        ),
      onCloseClicked = {},
    ) {
      Box(modifier = Modifier.fillMaxSize().background(Color.LightGray)) {
        Text(text = PAGER_CONTENT_TEXT, modifier = Modifier.align(Alignment.Center))
      }
    }
  }
}
