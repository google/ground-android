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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import org.groundplatform.android.R
import org.groundplatform.ui.theme.AppTheme
import org.groundplatform.ui.theme.sizes

@Composable
fun TaskViewLayout(
  modifier: Modifier = Modifier,
  header: Header? = null,
  footer: @Composable () -> Unit,
  content: @Composable () -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize()) {
    if (header != null) {
      TaskViewHeader(header)
    }
    Box(modifier = modifier.weight(1f).fillMaxWidth()) { content() }
    Box(modifier = Modifier.fillMaxWidth()) { footer() }
  }
}

/**
 * Data class representing the header section of a task view.
 *
 * @property label The text to be displayed in the header.
 * @property iconResId The optional resource ID of the icon to be displayed alongside the label.
 */
data class Header(val label: String, val iconResId: Int? = null)

@Composable
private fun TaskViewHeader(header: Header) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(MaterialTheme.sizes.taskViewPadding),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (header.iconResId != null) {
      Icon(
        painter = painterResource(id = header.iconResId),
        modifier = Modifier.padding(end = MaterialTheme.sizes.taskViewPadding),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        contentDescription = null,
      )
    }
    Text(
      text = header.label,
      color = MaterialTheme.colorScheme.onSurface,
      style = MaterialTheme.typography.bodyLarge,
    )
  }
}

@Preview(showBackground = true)
@Composable
fun TaskViewLayoutPreviewWithIcon() {
  AppTheme {
    TaskViewLayout(
      header = Header("Label 1", iconResId = R.drawable.ic_question_answer),
      footer = { Text("Footer 1") },
    ) {
      Text("Content 1")
    }
  }
}

@Preview(showBackground = true)
@Composable
fun TaskViewLayoutPreviewWithoutIcon() {
  AppTheme {
    TaskViewLayout(
      header = Header(label = "Label 2", iconResId = null),
      footer = { Text("Footer 2") },
    ) {
      Text("Content 2")
    }
  }
}

@Preview(showBackground = true)
@Composable
fun TaskViewLayoutPreviewWithoutHeader() {
  AppTheme { TaskViewLayout(header = null, footer = { Text("Footer 3") }) { Text("Content 3") } }
}
