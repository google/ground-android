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

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.common.LocationOfInterestHelper
import org.groundplatform.android.ui.components.ConfirmationDialog
import org.groundplatform.android.ui.util.getDefaultColor
import org.groundplatform.domain.model.User
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.job.Style
import org.groundplatform.domain.model.locationofinterest.AuditInfo
import org.groundplatform.domain.model.locationofinterest.LocationOfInterest
import org.groundplatform.domain.model.task.Task
import org.groundplatform.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoiJobSheet(
  state: SelectedLoiSheetData,
  onCollectClicked: () -> Unit,
  onDeleteClicked: (() -> Unit)? = null,
  onDismiss: () -> Unit,
  onShareClicked: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    dragHandle = { BottomSheetDefaults.DragHandle(width = 32.dp) },
  ) {
    ModalContents(
      loi = state.loi,
      canUserSubmitData = state.canCollectData,
      submissionCount = state.submissionCount,
      showDeleteLoiButton = state.showDeleteLoiButton,
      showShareButton = state.loiReport != null,
      onDeleteClicked = onDeleteClicked,
      onCollectClicked = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onCollectClicked() }
      },
      onShareClicked = onShareClicked,
    )
  }
}

@Composable
private fun ModalContents(
  loi: LocationOfInterest,
  canUserSubmitData: Boolean,
  submissionCount: Int,
  showDeleteLoiButton: Boolean,
  showShareButton: Boolean,
  onDeleteClicked: (() -> Unit)?,
  onCollectClicked: () -> Unit,
  onShareClicked: () -> Unit,
) {
  val resources = LocalContext.current.resources
  val loiHelper = remember(resources) { LocationOfInterestHelper(resources) }
  val showDeleteDialog = remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 50.dp)) {
    JobName(loiHelper = loiHelper, loi = loi)
    LoiHeader(loiHelper = loiHelper, loi = loi)
    SubmissionRow(
      loi = loi,
      submissionCount = submissionCount,
      canUserSubmitData = canUserSubmitData,
      showShareButton = showShareButton,
      onCollectClicked = onCollectClicked,
      onShareClicked = onShareClicked,
    )
    DeleteSiteSection(
      showDeleteLoiButton = showDeleteLoiButton,
      isPredefined = loi.isPredefined == true,
      onClick = { showDeleteDialog.value = true },
    )
  }

  DeleteConfirmationDialog(
    visible = showDeleteDialog.value,
    onConfirm = {
      showDeleteDialog.value = false
      onDeleteClicked?.invoke()
    },
    onDismiss = { showDeleteDialog.value = false },
  )
}

@Composable
private fun JobName(loiHelper: LocationOfInterestHelper, loi: LocationOfInterest) {
  loiHelper.getJobName(loi)?.let {
    Text(
      it,
      color = MaterialTheme.colorScheme.onSurface,
      style = MaterialTheme.typography.titleMedium,
    )
  }
}

@Composable
private fun LoiHeader(loiHelper: LocationOfInterestHelper, loi: LocationOfInterest) {
  Row(
    modifier = Modifier.padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      painter = painterResource(R.drawable.ic_ring_marker),
      contentDescription = stringResource(R.string.job_site_icon),
      modifier = Modifier.size(32.dp),
      tint = Color(loi.job.getDefaultColor()),
    )
    Spacer(modifier = Modifier.size(18.dp))
    Text(
      loiHelper.getDisplayLoiName(loi),
      color = MaterialTheme.colorScheme.onSurface,
      style = MaterialTheme.typography.headlineMedium,
    )
  }
}

@Composable
private fun SubmissionRow(
  loi: LocationOfInterest,
  submissionCount: Int,
  canUserSubmitData: Boolean,
  showShareButton: Boolean,
  onCollectClicked: () -> Unit,
  onShareClicked: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Top) {
    Text(
      if (submissionCount <= 0) stringResource(R.string.no_submissions)
      else pluralStringResource(R.plurals.submission_count, submissionCount, submissionCount),
      color = MaterialTheme.colorScheme.onSurface,
      style = MaterialTheme.typography.bodyLarge,
    )

    Row(
      modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
      horizontalArrangement = Arrangement.End,
    ) {
      if (showShareButton) {
        FilledTonalButton(onClick = onShareClicked) {
          Icon(
            modifier = Modifier.padding(end = 8.dp),
            imageVector = Icons.Outlined.Share,
            contentDescription = "Share",
          )
          Text(stringResource(R.string.share), modifier = Modifier.padding(4.dp))
        }
      }

      // NOTE(#2539): Avoid crash when there are no non-LOI tasks.
      val showAddData = canUserSubmitData && loi.job.hasNonLoiTasks() && loi.isPredefined == true
      if (showAddData) {
        Button(modifier = Modifier.padding(start = 8.dp), onClick = onCollectClicked) {
          Text(stringResource(R.string.add_data), modifier = Modifier.padding(4.dp))
        }
      }
    }
  }
}

@Composable
private fun DeleteSiteSection(
  showDeleteLoiButton: Boolean,
  isPredefined: Boolean,
  onClick: () -> Unit,
) {
  if (showDeleteLoiButton && !isPredefined) {
    Spacer(modifier = Modifier.size(16.dp))
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(R.string.delete_site))
    }
  }
}

@Composable
private fun DeleteConfirmationDialog(
  visible: Boolean,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  if (!visible) return
  ConfirmationDialog(
    title = R.string.delete_site_dialog_title,
    description = R.string.delete_site_dialog_message,
    confirmButtonText = R.string.delete,
    onConfirmClicked = onConfirm,
    onDismiss = onDismiss,
  )
}

/** Fake data for preview. */
private val user = User(id = "user", email = "user@email.com", displayName = "User")
private val auditInfo = AuditInfo(user)
private const val SURVEY_ID = "survey"
private const val TASK_ID = "task 1"

@SuppressLint("UnrememberedMutableState")
@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
private fun PreviewModalContentsWhenJobHasNoTasks() {
  val loi =
    LocationOfInterest(
      id = "1",
      surveyId = SURVEY_ID,
      job = Job(id = "job"),
      created = auditInfo,
      lastModified = auditInfo,
      geometry = Point(coordinates = Coordinates(lat = 20.0, lng = 20.0)),
    )
  AppTheme {
    ModalContents(
      loi = loi,
      canUserSubmitData = true,
      submissionCount = 0,
      showDeleteLoiButton = false,
      showShareButton = true,
      onDeleteClicked = null,
      onShareClicked = {},
      onCollectClicked = {},
    )
  }
}

@SuppressLint("UnrememberedMutableState")
@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
private fun PreviewModalContentsWhenUserCannotSubmitData() {
  val loi =
    LocationOfInterest(
      id = "1",
      surveyId = SURVEY_ID,
      job =
        Job(
          id = "job",
          style = Style(color = "#FFEE8C00"),
          tasks =
            mapOf(
              Pair(
                TASK_ID,
                Task(
                  id = TASK_ID,
                  index = 1,
                  type = Task.Type.TEXT,
                  label = "task",
                  isRequired = false,
                ),
              )
            ),
        ),
      created = auditInfo,
      lastModified = auditInfo,
      geometry = Point(coordinates = Coordinates(lat = 20.0, lng = 20.0)),
    )
  AppTheme {
    ModalContents(
      loi = loi,
      canUserSubmitData = false,
      submissionCount = 1,
      showDeleteLoiButton = false,
      showShareButton = true,
      onDeleteClicked = null,
      onShareClicked = {},
      onCollectClicked = {},
    )
  }
}

@SuppressLint("UnrememberedMutableState")
@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
private fun PreviewModalContentsWhenJobHasTasks() {
  val loi =
    LocationOfInterest(
      id = "1",
      surveyId = SURVEY_ID,
      job =
        Job(
          id = "job",
          style = Style(color = "#4169E1"),
          name = "Job name",
          tasks =
            mapOf(
              Pair(
                TASK_ID,
                Task(
                  id = TASK_ID,
                  index = 1,
                  type = Task.Type.TEXT,
                  label = "task",
                  isRequired = false,
                ),
              )
            ),
        ),
      created = auditInfo,
      lastModified = auditInfo,
      geometry = Point(coordinates = Coordinates(lat = 20.0, lng = 20.0)),
      isPredefined = false,
    )
  AppTheme {
    ModalContents(
      loi = loi,
      canUserSubmitData = true,
      submissionCount = 20,
      showDeleteLoiButton = false,
      showShareButton = true,
      onDeleteClicked = null,
      onShareClicked = {},
      onCollectClicked = {},
    )
  }
}

@SuppressLint("UnrememberedMutableState")
@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
private fun PreviewModalContentsWhenJobHasTasksAndIsPredefined() {
  val loi =
    LocationOfInterest(
      id = "1",
      surveyId = SURVEY_ID,
      job =
        Job(
          id = "job",
          style = Style(color = "#4169E1"),
          name = "Job name",
          tasks =
            mapOf(
              Pair(
                TASK_ID,
                Task(
                  id = TASK_ID,
                  index = 1,
                  type = Task.Type.TEXT,
                  label = "task",
                  isRequired = false,
                ),
              )
            ),
        ),
      created = auditInfo,
      lastModified = auditInfo,
      geometry = Point(coordinates = Coordinates(lat = 20.0, lng = 20.0)),
      isPredefined = true,
    )
  AppTheme {
    ModalContents(
      loi = loi,
      canUserSubmitData = true,
      submissionCount = 20,
      showDeleteLoiButton = true,
      showShareButton = true,
      onDeleteClicked = null,
      onShareClicked = {},
      onCollectClicked = {},
    )
  }
}
