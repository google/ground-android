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
package org.groundplatform.android.ui.datacollection.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.groundplatform.android.R
import org.groundplatform.android.ui.datacollection.LoiNameAction

const val LOI_NAME_TEXT_FIELD_TEST_TAG: String = "loi name text field test tag"

/**
 * Prompts for a name before creating a new location of interest.
 *
 * The window is full screen with [DialogProperties.decorFitsSystemWindows] disabled so IME insets
 * are delivered to Compose. [WindowInsets.safeDrawing] (system bars plus the IME, the same inset
 * used by other bottom-aligned UI) keeps Save and Cancel above the keyboard. The name field scrolls
 * when that region is shorter than the content.
 */
@Composable
fun LoiNameDialog(textFieldValue: String, onLoiNameAction: (LoiNameAction) -> Unit) {
  val currentAction = rememberUpdatedState(onLoiNameAction)
  Dialog(
    onDismissRequest = { currentAction.value(LoiNameAction.Dismissed) },
    properties = DialogProperties(decorFitsSystemWindows = false, usePlatformDefaultWidth = false),
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).pointerInput(Unit) {
          detectTapGestures { currentAction.value(LoiNameAction.Dismissed) }
        },
      contentAlignment = Alignment.Center,
    ) {
      LoiNameDialogCard(textFieldValue = textFieldValue, onLoiNameAction = currentAction.value)
    }
  }
}

@Composable
private fun LoiNameDialogCard(
  textFieldValue: String,
  onLoiNameAction: (LoiNameAction) -> Unit,
) {
  Surface(
    modifier =
      Modifier.padding(horizontal = 24.dp)
        .widthIn(min = 280.dp, max = 560.dp)
        .fillMaxWidth()
        .pointerInput(Unit) { detectTapGestures {} },
    shape = AlertDialogDefaults.shape,
    color = AlertDialogDefaults.containerColor,
    tonalElevation = AlertDialogDefaults.TonalElevation,
  ) {
    Column(modifier = Modifier.padding(24.dp)) {
      Column(
        modifier = Modifier.weight(weight = 1f, fill = false).verticalScroll(rememberScrollState())
      ) {
        LoiNameDialogTitle()
        Spacer(Modifier.height(16.dp))
        Text(
          text = stringResource(R.string.loi_name_dialog_body),
          color = AlertDialogDefaults.textContentColor,
          style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        TextField(
          value = textFieldValue,
          onValueChange = { onLoiNameAction(LoiNameAction.Changed(it)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag(LOI_NAME_TEXT_FIELD_TEST_TAG),
        )
      }
      Spacer(Modifier.height(24.dp))
      LoiNameDialogButtons(textFieldValue = textFieldValue, onLoiNameAction = onLoiNameAction)
    }
  }
}

@Composable
private fun LoiNameDialogTitle() {
  Text(
    text = stringResource(R.string.loi_name_dialog_title),
    color = AlertDialogDefaults.titleContentColor,
    style = MaterialTheme.typography.headlineSmall,
  )
}

@Composable
private fun LoiNameDialogButtons(
  textFieldValue: String,
  onLoiNameAction: (LoiNameAction) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.End,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    OutlinedButton(onClick = { onLoiNameAction(LoiNameAction.Dismissed) }) {
      Text(text = stringResource(R.string.cancel))
    }
    Spacer(Modifier.width(8.dp))
    Button(
      onClick = { onLoiNameAction(LoiNameAction.Confirmed(textFieldValue)) },
      contentPadding = PaddingValues(25.dp, 0.dp),
      enabled = textFieldValue.isNotBlank(),
    ) {
      Text(stringResource(R.string.save))
    }
  }
}
