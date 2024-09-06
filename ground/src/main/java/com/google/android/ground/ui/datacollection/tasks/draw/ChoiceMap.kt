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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.ground.R
import com.google.android.ground.ui.theme.AppTheme
import com.google.android.ground.ui.theme.card_background

@Composable
fun ChoiceMapDialog() {
  AlertDialog(
    onDismissRequest = {  },
    title = { Text(stringResource(id = R.string.choice_map_dialog)) },
    dismissButton = {
      OutlinedButton(onClick = {  }) { Text(text = stringResource(R.string.close)) }
    },
    confirmButton = { },
    text = {
      Row(
        modifier = Modifier
          .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        Text(text = stringResource(id = R.string.drop_a_pin),
          modifier = Modifier
            .weight(1f)
            .background(
              color = card_background,
              shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
          textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(id = R.string.draw_or_walk),
          modifier = Modifier
            .weight(1f)
          .background(
            color = card_background,
            shape = RoundedCornerShape(16.dp)
          )
          .padding(16.dp),
        textAlign = TextAlign.Center)
      }
    }
  )
}

@Preview
@Composable
fun ChoiceMapDialogPreview() {
  AppTheme {
    ChoiceMapDialog()
  }
}
