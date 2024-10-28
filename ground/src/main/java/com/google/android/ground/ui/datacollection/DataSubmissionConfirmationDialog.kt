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
package com.google.android.ground.ui.datacollection

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.ground.R
import com.google.android.ground.ui.theme.AppTheme

@Composable
fun DataSubmissionConfirmationDialog(onDismiss: () -> Unit) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
  ) {
    Surface(
      modifier = Modifier.wrapContentWidth().wrapContentHeight(),
      shape = RoundedCornerShape(16.dp),
      tonalElevation = 8.dp,
    ) {
      val configuration = LocalConfiguration.current
      if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            DataCollectionThumbnail(modifier = Modifier.weight(1f))
            Column(
              modifier = Modifier.padding(start = 16.dp, end = 24.dp, top = 16.dp).weight(2f),
              verticalArrangement = Arrangement.Center,
            ) {
              DetailColumn()
            }
          }
          CloseButton(modifier = Modifier.padding(bottom = 24.dp, end = 24.dp), onDismiss)
        }
      } else {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
          DataCollectionThumbnail()
          DetailColumn()
          Spacer(modifier = Modifier.height(12.dp))
          CloseButton(onDismiss = onDismiss)
        }
      }
    }
  }
}

@Composable
private fun DataCollectionThumbnail(modifier: Modifier = Modifier) {
  Image(
    modifier = modifier,
    painter = painterResource(id = R.drawable.data_submitted),
    contentDescription = stringResource(R.string.data_submitted_image),
    contentScale = ContentScale.Fit,
  )
}

@Composable
private fun ColumnScope.DetailColumn() {
  Text(
    text = stringResource(R.string.data_collection_complete),
    style = MaterialTheme.typography.titleLarge,
    color = MaterialTheme.colorScheme.onBackground,
    lineHeight = 28.sp,
  )
  Spacer(modifier = Modifier.height(8.dp))
  Text(
    text = stringResource(R.string.data_collection_complete_details),
    fontSize = 16.sp,
    lineHeight = 24.sp,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontFamily = FontFamily(Font(R.font.text_500)),
  )
}

@Composable
private fun ColumnScope.CloseButton(modifier: Modifier = Modifier, onDismiss: () -> Unit) {
  OutlinedButton(
    modifier = modifier.align(Alignment.End),
    border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline),
    onClick = { onDismiss() },
  ) {
    Text(
      stringResource(id = R.string.close),
      fontSize = 14.sp,
      lineHeight = 20.sp,
      fontFamily = FontFamily(Font(R.font.text_500)),
    )
  }
}

@Composable
@Preview(showBackground = false, heightDp = 360, widthDp = 800)
@Preview
fun DataSubmissionConfirmationDialogPreview() {
  AppTheme { DataSubmissionConfirmationDialog {} }
}
