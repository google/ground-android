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

package org.groundplatform.android.ui.home.mapcontainer.jobs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.groundplatform.android.R
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.Style
import org.groundplatform.android.ui.home.mapcontainer.jobs.JobMapComponentAction.OnAddDataClicked
import org.groundplatform.android.ui.home.mapcontainer.jobs.JobMapComponentAction.OnDeleteSiteClicked
import org.groundplatform.android.ui.home.mapcontainer.jobs.JobMapComponentAction.OnJobSelected
import org.groundplatform.android.ui.theme.AppTheme

@Composable
fun JobMapComponent(state: JobMapComponentState, onAction: (JobMapComponentAction) -> Unit) {
  var showJobSelectionModal by rememberSaveable { mutableStateOf(false) }
  LaunchedEffect(showJobSelectionModal) {
    onAction(JobMapComponentAction.OnJobSelectionModalVisibilityChanged(showJobSelectionModal))
  }

  state.adHocDataCollectionButtonData
    .takeIf { it.isNotEmpty() }
    ?.let { data ->
      if (showJobSelectionModal) {
        JobSelectionModal(
          jobs = data.map { it.job },
          onJobClicked = { job ->
            showJobSelectionModal = false
            onAction(OnJobSelected(job))
          },
          onDismiss = { showJobSelectionModal = false },
        )
      } else {
        AddLoiButton(
          onClick = {
            if (data.size > 1) {
              showJobSelectionModal = true
            } else {
              onAction(OnJobSelected(data.first().job))
            }
          }
        )
      }
    }
  state.selectedLoi?.let { loi ->
    LoiJobSheet(
      loi = loi.loi,
      canUserSubmitData = loi.canCollectData,
      submissionCount = loi.submissionCount,
      showDeleteLoiButton = loi.showDeleteLoiButton,
      onCollectClicked = { onAction(OnAddDataClicked(loi)) },
      onDeleteClicked = { onAction(OnDeleteSiteClicked(loi)) },
      onDismiss = { onAction(JobMapComponentAction.OnJobCardDismissed) },
    )
  }
}

@Composable
private fun AddLoiButton(onClick: () -> Unit) {
  Box {
    ActionButton(
      modifier =
        Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
          .align(Alignment.BottomCenter)
          .padding(bottom = 36.dp),
      icon = Icons.Filled.Add,
      contentDescription = stringResource(id = R.string.add_site),
      onClick = onClick,
    )
  }
}

data class JobMapComponentState(
  val selectedLoi: SelectedLoiSheetData? = null,
  val adHocDataCollectionButtonData: List<AdHocDataCollectionButtonData> = emptyList(),
)

sealed interface JobMapComponentAction {
  data class OnJobSelectionModalVisibilityChanged(val isShown: Boolean) : JobMapComponentAction

  data class OnJobSelected(val job: Job) : JobMapComponentAction

  data class OnAddDataClicked(val selectedLoi: SelectedLoiSheetData) : JobMapComponentAction

  data class OnDeleteSiteClicked(val selectedLoi: SelectedLoiSheetData) : JobMapComponentAction

  data object OnJobCardDismissed : JobMapComponentAction
}

@Preview
@Composable
private fun JobMapComponentPreview() {
  AppTheme {
    JobMapComponent(
      state =
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
        )
    ) {}
  }
}
