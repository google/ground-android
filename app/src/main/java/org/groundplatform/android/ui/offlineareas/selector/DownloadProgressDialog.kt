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

package org.groundplatform.android.ui.offlineareas.selector

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.groundplatform.android.R

@Composable
fun DownloadProgressDialog(progress: Float, onDismiss: () -> Unit) {
  AlertDialog(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    onDismissRequest = {},
    title = {
      Text(
        stringResource(
          R.string.offline_map_imagery_download_progress_dialog_title,
          (progress * 100).toInt(),
        ),
        color = MaterialTheme.colorScheme.onSurface,
        fontFamily = FontFamily(Font(R.font.text_500)),
      )
    },
    text = {
      Column {
        val animatedProgress by
          animateFloatAsState(targetValue = progress, animationSpec = tween(durationMillis = 300))

        LinearProgressIndicator(
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).testTag("progressBar"),
          progress = { animatedProgress },
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
          stringResource(R.string.offline_map_imagery_download_progress_dialog_message),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontFamily = FontFamily(Font(R.font.text_500)),
        )
      }
    },
    dismissButton = {
      TextButton(onClick = { onDismiss() }) {
        Text(text = stringResource(R.string.cancel), fontFamily = FontFamily(Font(R.font.text_500)))
      }
    },
    confirmButton = {},
  )
}
