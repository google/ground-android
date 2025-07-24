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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.groundplatform.android.R
import org.groundplatform.android.model.SurveyListItem

@Composable
fun SurveyCardItem(
  item: SurveyListItem,
  modifier: Modifier = Modifier,
  onCardClick: (String) -> Unit,
  menuClick: (String) -> Unit,
) {
  Card(
    modifier = modifier.fillMaxWidth().clickable { onCardClick(item.id) },
    shape = MaterialTheme.shapes.medium,
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
  ) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
      HeaderRow(item, menuClick)
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = item.title,
        fontFamily = FontFamily(Font(R.font.text_500)),
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 28.sp,
        color = MaterialTheme.colorScheme.onSurface,
      )
      item.description
        .takeIf { it.isNotEmpty() }
        ?.let {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = it,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily(Font(R.font.text_500)),
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
    }
  }
}
