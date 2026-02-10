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
package org.groundplatform.android.ui.datacollection.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.datacollection.tasks.location.LocationAccuracyCard
import org.groundplatform.android.ui.theme.AppTheme

@Composable
fun TaskFooter(
  modifier: Modifier = Modifier,
  headerCard: (@Composable () -> Unit)? = null,
  buttonActionStates: List<ButtonActionState>,
  onButtonClicked: (ButtonAction) -> Unit,
) {
  Column(modifier = modifier.padding(24.dp).fillMaxWidth()) {
    if (headerCard != null) {
      headerCard()
      Spacer(Modifier.height(12.dp))
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      buttonActionStates.forEach { state ->
        if (state.isVisible) {
          TaskButton(state = state, onClick = { onButtonClicked(state.action) })
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun TaskFooterNoHeaderPreview() {
  val actions =
    listOf(ButtonAction.PREVIOUS, ButtonAction.UNDO, ButtonAction.REDO, ButtonAction.NEXT)
  AppTheme {
    TaskFooter(buttonActionStates = actions.map { ButtonActionState(it) }, onButtonClicked = {})
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun TaskFooterWithHeaderPreview() {
  val actions =
    listOf(ButtonAction.PREVIOUS, ButtonAction.UNDO, ButtonAction.REDO, ButtonAction.NEXT)
  AppTheme {
    TaskFooter(
      headerCard = { LocationAccuracyCard(onDismiss = {}) },
      buttonActionStates = actions.map { ButtonActionState(it) },
      onButtonClicked = {},
    )
  }
}
