/*
 * Copyright 2024 Google LLC
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
package com.google.android.ground.ui.datacollection.tasks.draw

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.ground.R
import com.google.android.ground.ui.theme.AppTheme
import com.google.android.ground.ui.theme.card_background
import com.google.android.ground.ui.theme.md_theme_dark_outlineVariant

@Composable
fun SelectGeometryTaskDialog() {
  AlertDialog(
    onDismissRequest = {},
    title = { Text(stringResource(id = R.string.choice_map_dialog)) },
    dismissButton = {
      OutlinedButton(onClick = {}) { Text(text = stringResource(R.string.close)) }
    },
    confirmButton = {},
    text = {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        OptionsCard(textRes = R.string.drop_a_pin, imageRes = R.drawable.outline_pin_drop) {}
        Spacer(modifier = Modifier.width(8.dp))
        OptionsCard(textRes = R.string.draw_or_walk, imageRes = R.drawable.outline_pin_drop) {}
      }
    },
  )
}

@Composable
private fun RowScope.OptionsCard(
  @StringRes textRes: Int,
  @DrawableRes imageRes: Int,
  onClick: () -> Unit,
) {
  Column(
    modifier =
      Modifier.weight(1f)
        .height(100.dp)
        .background(color = card_background, shape = RoundedCornerShape(16.dp))
        .padding(16.dp)
        .clickable { onClick() },
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Image(painter = painterResource(id = imageRes), contentDescription = "")
    Spacer(modifier = Modifier.weight(1f))
    Text(
      text = stringResource(id = textRes),
      modifier = Modifier,
      textAlign = TextAlign.Center,
      color = md_theme_dark_outlineVariant,
    )
  }
}

@Preview
@Composable
fun ChoiceMapDialogPreview() {
  AppTheme { SelectGeometryTaskDialog() }
}
