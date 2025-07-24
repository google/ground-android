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
package org.groundplatform.android.ui.surveyselector.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.groundplatform.android.R

@Composable
fun SectionHeader(title: String, expanded: Boolean, isClickable: Boolean, onClick: () -> Unit) {
  val interactionSource = remember { MutableInteractionSource() }

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(
          enabled = isClickable,
          interactionSource = interactionSource,
          indication = if (isClickable) LocalIndication.current else null,
          onClick = onClick,
        ),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector =
        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
      contentDescription = if (expanded) "Collapse" else "Expand",
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.width(8.dp))
    Text(
      text = title,
      fontFamily = FontFamily(Font(R.font.text_500)),
      lineHeight = 16.sp,
      fontSize = 16.sp,
      fontWeight = FontWeight(500),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
