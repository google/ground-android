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
package org.groundplatform.android.ui.datacollection

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.theme.AppTheme

@Composable
fun DataSubmissionConfirmationScreen(onDismissed: () -> Unit) {
  if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
    Row(
      modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      DataSubmittedImage()
      BodyContent { onDismissed() }
    }
  } else {
    Column(
      modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
      verticalArrangement = Arrangement.SpaceEvenly,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      DataSubmittedImage()
      BodyContent { onDismissed() }
    }
  }
}

@Composable
private fun DataSubmittedImage() {
  Image(
    painter = painterResource(id = R.drawable.data_submitted),
    contentDescription = stringResource(R.string.data_submitted_image),
    contentScale = ContentScale.Fit,
  )
}

@Composable
private fun BodyContent(onDismiss: () -> Unit) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = stringResource(R.string.data_collection_complete),
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
      fontFamily = FontFamily(Font(R.font.text_500)),
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(30.dp))
    OutlinedButton(onClick = { onDismiss() }) {
      Text(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
        text = stringResource(id = R.string.close),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFamily = FontFamily(Font(R.font.text_500)),
      )
    }
  }
}

@Composable
@Preview(heightDp = 320, widthDp = 800)
@ExcludeFromJacocoGeneratedReport
private fun DataSubmissionConfirmationScreenPreview() {
  AppTheme { DataSubmissionConfirmationScreen {} }
}
