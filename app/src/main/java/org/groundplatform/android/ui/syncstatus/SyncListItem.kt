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
package org.groundplatform.android.ui.syncstatus

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import org.groundplatform.android.R
import org.groundplatform.android.model.mutation.Mutation
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.theme.AppTheme

@Composable
fun SyncListItem(modifier: Modifier, detail: SyncStatusDetail) {
  Column {
    Row(modifier.fillMaxWidth().padding(top = 8.dp, end = 24.dp, bottom = 8.dp, start = 16.dp)) {
      Column(modifier.weight(1f)) {
        val date = detail.timestamp
        Text(
          text = "${date.toFormattedDate()} • ${date.toFormattedTime()}",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 12.sp,
          lineHeight = 16.sp,
          fontWeight = FontWeight(500),
          letterSpacing = 0.1.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text = "${detail.label} • ${detail.subtitle}",
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
        Text(text = detail.description, style = textStyle)
        Spacer(Modifier.height(4.dp))
        Text(text = stringResource(id = detail.status.toLabel()), fontSize = 11.sp)
      }
      Column(modifier = modifier.padding(start = 16.dp).align(alignment = CenterVertically)) {
        StatusIcon(status = detail.status, modifier = Modifier)
      }
    }
    HorizontalDivider(
      color = MaterialTheme.colorScheme.outlineVariant,
      thickness = 1.dp,
      modifier = Modifier.padding(horizontal = 16.dp),
    )
  }
}

private fun Mutation.SyncStatus.isActiveProgress(): Boolean =
  this == Mutation.SyncStatus.IN_PROGRESS || this == Mutation.SyncStatus.MEDIA_UPLOAD_IN_PROGRESS

@Composable
private fun StatusIcon(status: Mutation.SyncStatus, modifier: Modifier = Modifier) {
  val tint = MaterialTheme.colorScheme.onSurfaceVariant

  if (status.isActiveProgress()) {
    CircularProgressIndicator(
      modifier = modifier.padding(1.dp).size(24.dp),
      strokeWidth = 2.dp,
      color = tint,
    )
  } else {
    Icon(
      painter = painterResource(id = status.toIcon()),
      contentDescription = null,
      tint = tint,
      modifier = modifier.padding(1.dp).size(24.dp),
    )
  }
}

@Composable
private fun Date.toFormattedDate(): String {
  val locale = LocalConfiguration.current.locales[0]
  val dateFormat = SimpleDateFormat("MMM d, yyyy", locale)
  return dateFormat.format(this)
}

@Composable
private fun Date.toFormattedTime(): String =
  DateFormat.getTimeFormat(LocalContext.current).format(this)

private fun Mutation.SyncStatus.toLabel(): Int =
  when (this) {
    Mutation.SyncStatus.PENDING -> R.string.pending
    Mutation.SyncStatus.MEDIA_UPLOAD_IN_PROGRESS -> R.string.uploading_photos
    Mutation.SyncStatus.IN_PROGRESS -> R.string.uploading_form_data
    Mutation.SyncStatus.COMPLETED -> R.string.uploaded
    Mutation.SyncStatus.FAILED -> R.string.pending_retry
    Mutation.SyncStatus.MEDIA_UPLOAD_PENDING -> R.string.photos_pending
    Mutation.SyncStatus.MEDIA_UPLOAD_AWAITING_RETRY -> R.string.photos_pending_retry
    Mutation.SyncStatus.UNKNOWN -> error("Unexpected status")
  }

private fun Mutation.SyncStatus.toIcon(): Int =
  when (this) {
    Mutation.SyncStatus.MEDIA_UPLOAD_AWAITING_RETRY,
    Mutation.SyncStatus.PENDING -> R.drawable.baseline_hourglass_empty_24
    Mutation.SyncStatus.MEDIA_UPLOAD_IN_PROGRESS,
    Mutation.SyncStatus.IN_PROGRESS -> R.drawable.ic_sync
    Mutation.SyncStatus.MEDIA_UPLOAD_PENDING -> R.drawable.baseline_check_24
    Mutation.SyncStatus.COMPLETED -> R.drawable.baseline_check_24
    Mutation.SyncStatus.FAILED -> R.drawable.outline_error_outline_24
    Mutation.SyncStatus.UNKNOWN -> error("Unexpected status")
  }

@Composable
@Preview(showBackground = true, showSystemUi = true)
@ExcludeFromJacocoGeneratedReport
private fun PreviewSyncListItem(
  detail: SyncStatusDetail =
    SyncStatusDetail(
      user = "Jane Doe",
      timestamp = Date(),
      label = "Map the farms",
      subtitle = "IDX21311",
      status = Mutation.SyncStatus.PENDING,
      description = "Lacuna Fund Cocoa Mapping",
    )
) {
  AppTheme { SyncListItem(Modifier, detail) }
}
