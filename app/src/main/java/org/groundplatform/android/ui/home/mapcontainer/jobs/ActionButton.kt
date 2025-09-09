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
@file:Suppress("MatchingDeclarationName")

package org.groundplatform.android.ui.home.mapcontainer.jobs

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.theme.AppTheme

enum class ButtonMode {
  PRIMARY,
  SECONDARY,
}

@Composable
fun ActionButton(
  icon: ImageVector,
  contentDescription: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
  mode: ButtonMode = ButtonMode.PRIMARY,
) {
  Button(
    onClick = onClick,
    modifier = modifier.size(width = 100.dp, height = 100.dp),
    colors = getActionButtonColors(mode),
    shape = RoundedCornerShape(25),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      modifier = Modifier.size((36 * 160 / LocalDensity.current.density).dp),
    )
  }
}

@Composable
private fun getActionButtonColors(mode: ButtonMode) =
  when (mode) {
    ButtonMode.PRIMARY ->
      ButtonDefaults.buttonColors()
        .copy(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onSurface,
        )
    ButtonMode.SECONDARY ->
      ButtonDefaults.buttonColors()
        .copy(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSurface,
        )
  }

@Composable
@Preview
@ExcludeFromJacocoGeneratedReport
fun PreviewActionButton() {
  AppTheme {
    ActionButton(
      icon = Icons.Filled.Add,
      contentDescription = stringResource(id = R.string.add_site),
      onClick = {},
    )
  }
}
