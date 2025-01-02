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
package com.google.android.ground.ui.datacollection.tasks.text

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.ground.ExcludeFromJacocoGeneratedReport
import com.google.android.ground.ui.theme.AppTheme

const val INPUT_TEXT_TEST_TAG: String = "text task input test tag"

@Composable
fun TextTaskInput(
  value: String,
  modifier: Modifier = Modifier,
  valueChanged: (text: String) -> Unit = {},
) {
  TextField(
    value = value,
    onValueChange = { valueChanged(it) },
    modifier =
      modifier
        .wrapContentWidth(align = Alignment.Start)
        .wrapContentHeight(align = Alignment.Top)
        // TODO: Add horizontal padding as 16.dp when global padding is removed.
        // Issue URL: https://github.com/google/ground-android/issues/2976
        .padding(vertical = 8.dp)
        .testTag(INPUT_TEXT_TEST_TAG),
    textStyle = MaterialTheme.typography.bodyLarge,
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
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
