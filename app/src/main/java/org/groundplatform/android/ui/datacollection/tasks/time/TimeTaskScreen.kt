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
package org.groundplatform.android.ui.datacollection.tasks.time

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

const val TIME_TEXT_TEST_TAG: String = "time task input test tag"

// TODO: Add trailing icon (close logo) for clearing selected time.

@Composable
fun TimeTaskScreen(
  timeText: String,
  hintText: String,
  onTimeClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  LaunchedEffect(interactionSource) {
    interactionSource.interactions.collect { interaction ->
      if (interaction is PressInteraction.Release) {
        onTimeClick()
      }
    }
  }

  Column(modifier = modifier) {
    // TODO: Replace with simple text field.
    OutlinedTextField(
      value = timeText,
      onValueChange = {},
      readOnly = true,
      placeholder = { Text(hintText) },
      modifier = Modifier.width(200.dp).testTag(TIME_TEXT_TEST_TAG),
      interactionSource = interactionSource,
    )
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun TimeTaskScreenPreview() {
  AppTheme {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      TimeTaskScreen(timeText = "", hintText = "HH:MM AM", onTimeClick = {})
      Spacer(modifier = Modifier.height(10.dp))
      TimeTaskScreen(timeText = "10:30 AM", hintText = "HH:MM AM", onTimeClick = {})
    }
  }
}
