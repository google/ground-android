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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.groundplatform.android.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.R
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.Style
import org.groundplatform.android.model.job.getDefaultColor
import org.groundplatform.android.ui.theme.AppTheme

@Composable
fun JobSelectionRow(job: Job, onClick: () -> Unit) {
  Button(
    onClick = { onClick() },
    modifier = Modifier.fillMaxWidth(0.65F).clickable { onClick() },
    shape = RoundedCornerShape(25),
    colors =
      ButtonDefaults.buttonColors()
        .copy(
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = MaterialTheme.colorScheme.onSurface,
        ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Start,
    ) {
      Icon(
        painter = painterResource(R.drawable.ic_ring_marker),
        contentDescription = stringResource(R.string.job_site_icon),
        modifier = Modifier.size(32.dp),
        tint = Color(job.getDefaultColor()),
      )
      Spacer(modifier = Modifier.size(8.dp))
      Text(
        job.name ?: stringResource(R.string.unnamed_job),
        modifier = Modifier.padding(16.dp),
        fontSize = 24.sp,
      )
    }
  }
}

@Composable
@Preview(showSystemUi = true)
@ExcludeFromJacocoGeneratedReport
fun PreviewJobSelectionRow() {
  AppTheme {
    Column {
      Spacer(modifier = Modifier.size(60.dp))

      // Missing job style
      JobSelectionRow(job = Job(id = "1", name = "job name")) {}
      Spacer(modifier = Modifier.size(20.dp))

      // Missing job name
      JobSelectionRow(job = Job(id = "1")) {}
      Spacer(modifier = Modifier.size(20.dp))

      // With job name and style
      JobSelectionRow(job = Job(id = "1", style = Style(color = "#FFA500"), name = "job name")) {}
    }
  }
}
