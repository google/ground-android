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
package org.groundplatform.android.ui.datacollection.components.refactor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.theme.AppTheme

@Composable
fun TaskButton(
  modifier: Modifier = Modifier,
  state: ButtonActionState,
  onClick: (ButtonAction) -> Unit,
) {
  when (state.action.theme) {
    ButtonAction.Theme.DARK_GREEN ->
      Button(modifier = modifier, onClick = { onClick(state.action) }, enabled = state.isEnabled) {
        Content(action = state.action)
      }
    ButtonAction.Theme.LIGHT_GREEN ->
      FilledTonalButton(
        modifier = modifier,
        onClick = { onClick(state.action) },
        enabled = state.isEnabled,
      ) {
        Content(action = state.action)
      }
    ButtonAction.Theme.OUTLINED ->
      OutlinedButton(
        modifier = modifier,
        onClick = { onClick(state.action) },
        enabled = state.isEnabled,
      ) {
        Content(action = state.action)
      }
    ButtonAction.Theme.TRANSPARENT ->
      OutlinedButton(
        modifier = modifier,
        border = null,
        onClick = { onClick(state.action) },
        enabled = state.isEnabled,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
      ) {
        Content(action = state.action)
      }
  }
}

@Composable
private fun Content(modifier: Modifier = Modifier, action: ButtonAction) {
  when {
    action.drawableId != null -> {
      Icon(
        modifier = modifier,
        imageVector = ImageVector.vectorResource(id = action.drawableId),
        contentDescription = action.contentDescription?.let { resId -> stringResource(resId) },
      )
    }
    action.textId != null -> {
      Text(modifier = modifier, text = stringResource(id = action.textId))
    }
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun TaskButtonAllPreview() {
  AppTheme {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      ButtonAction.entries.forEach { action ->
        TaskButton(state = ButtonActionState(action), onClick = {})
      }
    }
  }
}
