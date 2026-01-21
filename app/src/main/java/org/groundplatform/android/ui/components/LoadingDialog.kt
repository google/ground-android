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
package org.groundplatform.android.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.theme.AppTheme
import org.groundplatform.android.ui.theme.sizes

@Composable
fun LoadingDialog(messageId: Int) {
  Dialog(onDismissRequest = {}) {
    Surface(
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 4.dp,
    ) {
      Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
          modifier = Modifier.size(MaterialTheme.sizes.progressIndicatorSize),
          color = MaterialTheme.colorScheme.primary,
          strokeWidth = MaterialTheme.sizes.progressIndicatorStrokeWidth,
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = stringResource(messageId), style = MaterialTheme.typography.bodyLarge)
      }
    }
  }
}

@Composable
@ExcludeFromJacocoGeneratedReport
@Preview(showBackground = true)
private fun PreviewLoadingDialog() {
  AppTheme { LoadingDialog(R.string.loading) }
}
