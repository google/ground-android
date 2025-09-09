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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.model.AuditInfo
import org.groundplatform.android.model.User
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.Style
import org.groundplatform.android.model.job.getDefaultColor
import org.groundplatform.android.model.locationofinterest.LocationOfInterest
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.common.LocationOfInterestHelper
import org.groundplatform.android.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoiJobSheet(
  loi: LocationOfInterest,
  canUserSubmitData: Boolean,
  submissionCount: Int,
  onCollectClicked: () -> Unit,
  onDismiss: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val sheetState = rememberModalBottomSheetState()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    dragHandle = { BottomSheetDefaults.DragHandle(width = 32.dp) },
  ) {
    ModalContents(loi, canUserSubmitData, submissionCount) {
      scope.launch { sheetState.hide() }.invokeOnCompletion { onCollectClicked() }
    }
  }
}

@Composable
private fun ModalContents(
  loi: LocationOfInterest,
  canUserSubmitData: Boolean,
  submissionCount: Int,
  onCollectClicked: () -> Unit,
) {
  val loiHelper = LocationOfInterestHelper(LocalContext.current.resources)

  Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 50.dp)) {

    // Job Name
    loiHelper.getJobName(loi)?.let {
      Text(it, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp)
    }

    // Icon & LOI Name
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
        fontSize = 28.sp,
      )
    }

    // Submission count & "Add data" button
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        if (submissionCount <= 0) stringResource(R.string.no_submissions)
        else pluralStringResource(R.plurals.submission_count, submissionCount, submissionCount),
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 16.sp,
      )
      // NOTE(#2539): The DataCollectionFragment will crash if there are no non-LOI tasks.
      if (canUserSubmitData && loi.job.hasNonLoiTasks() && loi.isPredefined == true) {
        Button(onClick = onCollectClicked) {
          Text(
            stringResource(R.string.add_data),
            modifier = Modifier.padding(4.dp),
            fontSize = 18.sp,
          )
        }
      }
    }
  }
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
fun PreviewModalContentsWhenJobHasNoTasks() {
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
    ModalContents(loi = loi, canUserSubmitData = true, submissionCount = 0, onCollectClicked = {})
  }
}

@SuppressLint("UnrememberedMutableState")
@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PreviewModalContentsWhenUserCannotSubmitData() {
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
    ModalContents(loi = loi, canUserSubmitData = false, submissionCount = 1, onCollectClicked = {})
  }
}

@SuppressLint("UnrememberedMutableState")
@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PreviewModalContentsWhenJobHasTasks() {
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
    ModalContents(loi = loi, canUserSubmitData = true, submissionCount = 20, onCollectClicked = {})
  }
}

@SuppressLint("UnrememberedMutableState")
@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PreviewModalContentsWhenJobHasTasksAndIsPredefined() {
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
    ModalContents(loi = loi, canUserSubmitData = true, submissionCount = 20, onCollectClicked = {})
  }
}
