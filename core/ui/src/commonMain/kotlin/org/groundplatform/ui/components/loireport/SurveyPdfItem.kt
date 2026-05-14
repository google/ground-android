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
package org.groundplatform.ui.components.loireport

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.annotation.VisibleForTesting
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ground_android.core.ui.generated.resources.Res
import ground_android.core.ui.generated.resources.ic_pdf
import ground_android.core.ui.generated.resources.pdf_details_data_collector
import ground_android.core.ui.generated.resources.pdf_details_site
import org.groundplatform.ui.components.ShareButton
import org.groundplatform.ui.theme.AppTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@VisibleForTesting const val TEST_TAG_PDF_ITEM = "TEST_TAG_SUBMISSION_PDF_ITEM"

@Composable
fun SubmissionPdfItem(
  modifier: Modifier = Modifier,
  title: String,
  loiName: String,
  userName: String,
  date: String,
  onItemClick: () -> Unit,
  onShareClick: () -> Unit,
) {
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
        .padding(24.dp)
        .testTag(TEST_TAG_PDF_ITEM)
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onItemClick)) {
        Image(
          modifier = Modifier.size(24.dp),
          painter = painterResource(Res.drawable.ic_pdf),
          contentDescription = null,
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
          Text(
            modifier = Modifier.padding(bottom = 8.dp),
            text = title,
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = stringResource(Res.string.pdf_details_site, loiName),
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            text = stringResource(Res.string.pdf_details_data_collector, userName),
            style = MaterialTheme.typography.bodySmall,
          )
          Text(text = date, style = MaterialTheme.typography.bodySmall)
        }
      }

      ShareButton(
        modifier = Modifier.padding(top = 24.dp).align(Alignment.CenterHorizontally),
        onClick = onShareClick,
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun SubmissionPdfItemPreview() {
  AppTheme {
    Surface {
      SubmissionPdfItem(
        modifier = Modifier.padding(16.dp),
        title = "Map restoration areas",
        loiName = "PX074662",
        userName = "Patricia Martinez",
        date = "Feb 14, 2026",
        onItemClick = {},
        onShareClick = {},
      )
    }
  }
}
