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
package org.groundplatform.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ground_android.core.ui.generated.resources.Res
import ground_android.core.ui.generated.resources.ic_share
import ground_android.core.ui.generated.resources.share
import org.groundplatform.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun ShareButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
  FilledTonalButton(modifier = modifier, onClick = onClick) {
    Icon(
      modifier = Modifier.padding(end = 8.dp),
      imageVector = vectorResource(Res.drawable.ic_share),
      contentDescription = "Share",
    )
    Text(stringResource(Res.string.share), modifier = Modifier.padding(4.dp))
  }
}

@Preview
@Composable
private fun ShareButtonPreview() {
  AppTheme { ShareButton(onClick = {}) }
}
