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

package org.groundplatform.android.ui.home.mapcontainer.jobs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.theme.AppTheme
import org.groundplatform.android.ui.theme.sizes

@Composable
fun ActionButton(
  modifier: Modifier = Modifier,
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  mode: ButtonMode = ButtonMode.PRIMARY,
) {
  IconButton(
    modifier = modifier.size(MaterialTheme.sizes.jobActionButtonSize),
    colors = getActionButtonColors(mode),
    shape = RoundedCornerShape(25),
    onClick = onClick,
  ) {
    Icon(
      modifier = Modifier.fillMaxSize(.5f),
      imageVector = icon,
      contentDescription = contentDescription,
    )
  }
}

@Composable
private fun getActionButtonColors(mode: ButtonMode) =
  when (mode) {
    ButtonMode.PRIMARY ->
      IconButtonDefaults.iconButtonColors()
        .copy(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onSurface,
        )
    ButtonMode.SECONDARY ->
      IconButtonDefaults.iconButtonColors()
        .copy(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSurface,
        )
  }

enum class ButtonMode {
  PRIMARY,
  SECONDARY,
}

@Composable
@Preview
@ExcludeFromJacocoGeneratedReport
private fun PreviewPrimaryActionButton() {
  AppTheme {
    ActionButton(
      icon = Icons.Filled.Add,
      contentDescription = stringResource(id = R.string.add_site),
      mode = ButtonMode.PRIMARY,
      onClick = {},
    )
  }
}

@Composable
@Preview
@ExcludeFromJacocoGeneratedReport
private fun PreviewSecondaryActionButton() {
  AppTheme {
    ActionButton(
      icon = Icons.Filled.Close,
      contentDescription = stringResource(id = R.string.close),
      mode = ButtonMode.SECONDARY,
      onClick = {},
    )
  }
}
