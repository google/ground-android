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
package org.groundplatform.android.ui.datacollection.tasks.date

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.theme.AppTheme

@Composable
fun DateTaskScreen(
  dateText: String,
  hintText: String,
  onDateClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    OutlinedTextField(
      value = dateText,
      onValueChange = {},
      readOnly = true,
      placeholder = { Text(hintText) },
      modifier = Modifier.width(200.dp).testTag("dateInputText"),
      interactionSource =
        remember { MutableInteractionSource() }
          .also { interactionSource ->
            LaunchedEffect(interactionSource) {
              interactionSource.interactions.collect {
                if (it is PressInteraction.Release) {
                  onDateClick()
                }
              }
            }
          },
    )
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
fun DateTaskScreenPreview() {
  AppTheme {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      DateTaskScreen(dateText = "", hintText = "DD/MM/YYYY", onDateClick = {})
      Spacer(modifier = Modifier.height(10.dp))
      DateTaskScreen(dateText = "14/02/2026", hintText = "DD/MM/YYYY", onDateClick = {})
    }
  }
}
