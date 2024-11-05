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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.ground.R
import com.google.android.ground.ui.theme.AppTheme

@Composable
fun DataSubmissionConfirmationDialog(onDismiss: () -> Unit) {
  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Spacer(modifier = Modifier.height(150.dp))
    DataCollectionThumbnail(modifier = Modifier.padding(horizontal = 8.dp))
    Text(
      modifier = Modifier.padding(horizontal = 8.dp),
      text = stringResource(R.string.data_collection_thumbnail_attribution),
      color = MaterialTheme.colorScheme.secondary,
      fontSize = 11.sp,
      fontWeight = FontWeight(400),
      lineHeight = 16.sp,
    )
    Spacer(modifier = Modifier.height(100.dp))
    DetailColumn()
    Spacer(modifier = Modifier.height(32.dp))
    CloseButton(modifier = Modifier.align(Alignment.CenterHorizontally), onDismiss = onDismiss)
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
private fun DetailColumn() {
  Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = stringResource(R.string.data_collection_complete),
      color = MaterialTheme.colorScheme.onSurface,
      fontFamily = FontFamily(Font(R.font.text_500)),
      lineHeight = 28.sp,
      fontSize = 22.sp,
      fontWeight = FontWeight(400),
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      modifier = Modifier.padding(horizontal = 28.dp),
      text = stringResource(R.string.data_collection_complete_details),
      fontSize = 14.sp,
      lineHeight = 20.sp,
      fontWeight = FontWeight(400),
      color = MaterialTheme.colorScheme.onSurface,
      fontFamily = FontFamily(Font(R.font.text_500)),
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun CloseButton(modifier: Modifier = Modifier, onDismiss: () -> Unit) {
  OutlinedButton(
    modifier = modifier,
    border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline),
    onClick = { onDismiss() },
  ) {
    Text(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
      text = stringResource(id = R.string.close),
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
