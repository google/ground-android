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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.groundplatform.android.R
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.ui.home.mapcontainer.jobs.JobMapComponentAction.OnAddDataClicked
import org.groundplatform.android.ui.home.mapcontainer.jobs.JobMapComponentAction.OnDeleteSiteClicked
import org.groundplatform.android.ui.home.mapcontainer.jobs.JobMapComponentAction.OnJobSelected

@Composable
fun JobMapComponent(
  state: JobMapComponentState,
  onAction: (JobMapComponentAction) -> Unit,
) {
  var showJobSelectionModal by rememberSaveable { mutableStateOf(false) }

    when (state) {
      is JobMapComponentState.AddNewLoi ->
        if (showJobSelectionModal) {
          JobSelectionModal(
            jobs = state.data.map { it.job },
            onJobClicked = { job ->
              showJobSelectionModal = false
              onAction(OnJobSelected(job))
            },
            onDismiss = { showJobSelectionModal = false },
          )
        } else {
          AddLoiButton(
            onClick = {
              if (state.data.size > 1) {
                showJobSelectionModal = true
              } else {
                onAction(OnJobSelected(state.data.first().job))
              }
            }
          )
        }

      is JobMapComponentState.ShowLoiJobSheet ->
        LoiJobSheet(
          loi = state.data.loi,
          canUserSubmitData = state.data.canCollectData,
          submissionCount = state.data.submissionCount,
          showDeleteLoiButton = state.data.showDeleteLoiButton,
          onCollectClicked = { onAction(OnAddDataClicked(state.data)) },
          onDeleteClicked = { onAction(OnDeleteSiteClicked(state.data)) },
          onDismiss = { onAction(JobMapComponentAction.OnJobCardDismissed) },
        )

      JobMapComponentState.Empty ->
        return
    }
}

@Composable
private fun AddLoiButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
  Row(
    modifier.fillMaxWidth().padding(bottom = 36.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ActionButton(
      icon = Icons.Filled.Add,
      contentDescription = stringResource(id = R.string.add_site),
      onClick = onClick,
    )
  }
}

sealed class JobMapComponentState {
  data class AddNewLoi(val data: List<AdHocDataCollectionButtonData>) : JobMapComponentState()

  data class ShowLoiJobSheet(val data: SelectedLoiSheetData) : JobMapComponentState()
  data object Empty: JobMapComponentState()
}

sealed interface JobMapComponentAction {
  data class OnJobSelected(val job: Job) : JobMapComponentAction
  data class OnAddDataClicked(val selectedLoi: SelectedLoiSheetData) : JobMapComponentAction

  data class OnDeleteSiteClicked(val selectedLoi: SelectedLoiSheetData) : JobMapComponentAction
  data object OnJobCardDismissed : JobMapComponentAction
}