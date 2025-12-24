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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.groundplatform.android.R
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.Style
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.theme.AppTheme

@Composable
fun JobSelectionModal(jobs: List<Job>, onJobClicked: (job: Job) -> Unit, onDismiss: () -> Unit) {
  Column(
    Modifier.fillMaxWidth()
      .background(color = Color.Black.copy(alpha = 0.6f))
      .pointerInput(Unit) { detectTapGestures {} }
      .clickable(onClick = onDismiss)
  ) {
    Column(
      Modifier.fillMaxWidth().weight(1F),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      ShowJobCards(jobs, onJobClicked)
    }
    ActionButton(
      modifier =
        Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
          .align(Alignment.CenterHorizontally)
          .padding(bottom = 36.dp),
      icon = Icons.Filled.Clear,
      contentDescription = stringResource(R.string.close),
      onClick = onDismiss,
      mode = ButtonMode.SECONDARY,
    )
  }
}

@Composable
private fun ShowJobCards(jobs: List<Job>, onJobClicked: (job: Job) -> Unit) {
  jobs.forEach { job ->
    JobSelectionRow(job) { onJobClicked(job) }
    Spacer(Modifier.height(16.dp))
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
private fun PreviewJobSelectionModal() {
  AppTheme {
    JobSelectionModal(
      jobs =
        listOf(
          Job(id = "1", style = Style(color = "#4169E1"), name = "job 1", tasks = emptyMap()),
          Job(id = "2", style = Style(color = "#FFA500"), name = "job 2", tasks = emptyMap()),
        ),
      onJobClicked = {},
      onDismiss = {},
    )
  }
}
