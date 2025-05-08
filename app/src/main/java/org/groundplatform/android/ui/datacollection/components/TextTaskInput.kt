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
package org.groundplatform.android.ui.datacollection.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.groundplatform.android.Config
import org.groundplatform.android.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.theme.AppTheme

@Composable
fun TextTaskInput(
  value: String,
  modifier: Modifier = Modifier,
  keyboardType: KeyboardType = KeyboardType.Text,
  valueChanged: (text: String) -> Unit = {},
) {
  OutlinedTextField(
    supportingText = {
      Row(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = modifier.weight(1f))
        Text("${value.length} / ${Config.TEXT_DATA_CHAR_LIMIT}", textAlign = TextAlign.End)
      }
    },
    isError = value.length > Config.TEXT_DATA_CHAR_LIMIT,
    value = value,
    onValueChange = { valueChanged(it) },
    modifier =
      modifier
        .fillMaxWidth()
        .wrapContentHeight(align = Alignment.Top)
        // TODO: Add horizontal padding as 16.dp when global padding is removed.
        // Issue URL: https://github.com/google/ground-android/issues/2976
        .padding(vertical = 8.dp),
    textStyle = MaterialTheme.typography.bodyLarge,
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
  )
}

@Preview
@Composable
@ExcludeFromJacocoGeneratedReport
fun TextTaskInputPreview() {
  AppTheme { TextTaskInput(value = "Some value") }
}

@Preview
@Composable
@ExcludeFromJacocoGeneratedReport
fun TextTaskInputEmptyPreview() {
  AppTheme { TextTaskInput(value = "") }
}
