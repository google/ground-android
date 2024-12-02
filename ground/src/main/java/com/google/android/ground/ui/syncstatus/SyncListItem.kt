/*
 * Copyright 2024 Google LLC
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
package com.google.android.ground.ui.syncstatus

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.ground.ExcludeFromJacocoGeneratedReport
import com.google.android.ground.R
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.ui.theme.AppTheme
import java.util.Date

@Composable
fun SyncListItem(modifier: Modifier, detail: MutationDetail) {
  Column {
    Row(modifier.fillMaxWidth().padding(top = 8.dp, end = 24.dp, bottom = 8.dp, start = 16.dp)) {
      Column(modifier.weight(1f)) {
        val date = detail.mutation.clientTimestamp
        Text(
          text = "${date.toFormattedDate()} • ${date.toFormattedTime()}",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 12.sp,
          lineHeight = 16.sp,
          fontWeight = FontWeight(500),
          letterSpacing = 0.1.sp,
        )
        Text(
          text = detail.user,
          style =
            TextStyle(
              fontSize = 16.sp,
              lineHeight = 24.sp,
              fontFamily = FontFamily(Font(R.font.text_500)),
              color = MaterialTheme.colorScheme.onSurface,
            ),
        )
        val textStyle =
          TextStyle(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight(400),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        Text(text = detail.loiLabel, style = textStyle)
        Text(text = detail.loiSubtitle, style = textStyle)
      }
      Column(modifier = modifier.padding(start = 16.dp).align(alignment = CenterVertically)) {
        Row(verticalAlignment = CenterVertically) {
          Text(text = stringResource(id = detail.mutation.syncStatus.toLabel()), fontSize = 11.sp)
          Spacer(modifier = Modifier.width(10.dp))
          Icon(
            imageVector = ImageVector.vectorResource(id = detail.mutation.syncStatus.toIcon()),
            contentDescription = "",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(1.dp).width(24.dp).height(24.dp),
          )
        }
      }
    }
    HorizontalDivider(
      color = MaterialTheme.colorScheme.outlineVariant,
      thickness = 1.dp,
      modifier = Modifier.padding(horizontal = 16.dp),
    )
  }
}

@Composable
private fun Date.toFormattedDate(): String =
  DateFormat.getDateFormat(LocalContext.current).format(this)

@Composable
private fun Date.toFormattedTime(): String =
  DateFormat.getTimeFormat(LocalContext.current).format(this)

private fun Mutation.SyncStatus.toLabel(): Int =
  when (this) {
    Mutation.SyncStatus.PENDING -> R.string.upload_pending
    Mutation.SyncStatus.MEDIA_UPLOAD_IN_PROGRESS -> R.string.media_syncing
    Mutation.SyncStatus.IN_PROGRESS -> R.string.syncing
    Mutation.SyncStatus.COMPLETED -> R.string.synced
    Mutation.SyncStatus.FAILED -> R.string.failed
    Mutation.SyncStatus.MEDIA_UPLOAD_PENDING -> R.string.data_synced_media_pending
    Mutation.SyncStatus.MEDIA_UPLOAD_AWAITING_RETRY -> R.string.data_synced_media_pending_retry
    Mutation.SyncStatus.UNKNOWN -> error("Unexpected status")
  }

private fun Mutation.SyncStatus.toIcon(): Int =
  when (this) {
    Mutation.SyncStatus.MEDIA_UPLOAD_AWAITING_RETRY,
    Mutation.SyncStatus.PENDING -> R.drawable.baseline_hourglass_empty_24
    Mutation.SyncStatus.MEDIA_UPLOAD_IN_PROGRESS,
    Mutation.SyncStatus.IN_PROGRESS -> R.drawable.ic_sync
    Mutation.SyncStatus.MEDIA_UPLOAD_PENDING -> R.drawable.baseline_check_24
    Mutation.SyncStatus.COMPLETED -> R.drawable.outline_done_all_24
    Mutation.SyncStatus.FAILED -> R.drawable.outline_error_outline_24
    Mutation.SyncStatus.UNKNOWN -> error("Unexpected status")
  }

@Composable
@Preview(showBackground = true, showSystemUi = true)
@ExcludeFromJacocoGeneratedReport
fun PreviewSyncListItem(
  detail: MutationDetail =
    MutationDetail(
      user = "Jane Doe",
      loiLabel = "Map the farms",
      loiSubtitle = "IDX21311",
      mutation =
        SubmissionMutation(
          job = Job(id = "123"),
          syncStatus = Mutation.SyncStatus.PENDING,
          collectionId = "example",
        ),
    )
) {
  AppTheme { SyncListItem(Modifier, detail) }
}
