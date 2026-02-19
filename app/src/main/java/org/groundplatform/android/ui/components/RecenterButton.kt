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
package org.groundplatform.android.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.groundplatform.android.R
import org.groundplatform.android.ui.theme.AppTheme

@Composable
fun RecenterButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
  OutlinedButton(
    modifier = modifier,
    colors =
      ButtonDefaults.buttonColors()
        .copy(
          containerColor = MaterialTheme.colorScheme.background,
          contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    onClick = onClick,
  ) {
    Icon(
      modifier = Modifier.padding(end = 8.dp),
      imageVector = ImageVector.vectorResource(R.drawable.ic_gps_lock),
      tint = MaterialTheme.colorScheme.primary,
      contentDescription = null,
    )

    Text(text = stringResource(R.string.recenter))
  }
}

@Preview
@Composable
private fun RecenterButtonPreview() {
  AppTheme { RecenterButton(onClick = {}) }
}
