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
package com.google.android.ground.ui.datacollection.components

import android.util.TypedValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.google.android.ground.R

@Composable
@ReadOnlyComposable
private fun getColor(id: Int): Color = colorResource(id)

// `colorAttribute` implementation adapted from:
// https://stackoverflow.com/questions/62971113/how-to-reference-theme-attributes-in-jetpack-compose.
@Composable
@ReadOnlyComposable
fun colorAttribute(attrColor: Int) =
  colorResource(
    TypedValue()
      .apply { LocalContext.current.theme.resolveAttribute(attrColor, this, true) }
      .resourceId
  )

@Composable
private fun getElementColors(): Triple<ButtonColors, Color, TextFieldColors> {
  val primaryColor = colorAttribute(R.attr.colorPrimary)
  val onPrimaryColor = getColor(R.color.md_theme_onPrimary)
  val onSurfaceDisabledColor = getColor(R.color.md_theme_on_surface_disabled)
  val textFieldColor = getColor(R.color.md_theme_textFieldContainers)
  val saveButtonColors =
    ButtonColors(
      containerColor = primaryColor,
      contentColor = onPrimaryColor,
      disabledContainerColor = onSurfaceDisabledColor,
      disabledContentColor = onPrimaryColor,
    )
  val cancelButtonColor = colorAttribute(R.attr.colorPrimary)
  val textFieldColors =
    TextFieldDefaults.colors(
      focusedIndicatorColor = primaryColor,
      unfocusedIndicatorColor = primaryColor,
      focusedContainerColor = textFieldColor,
      unfocusedContainerColor = textFieldColor,
      cursorColor = primaryColor,
    )

  return Triple(saveButtonColors, cancelButtonColor, textFieldColors)
}

@Composable
fun LoiNameDialog(
  textFieldValue: String,
  onConfirmRequest: () -> Unit,
  onDismissRequest: () -> Unit,
  onTextFieldChange: (String) -> Unit
) {
  val (saveButtonColors, cancelButtonColor, textFieldColors) = getElementColors()
  AlertDialog(
    onDismissRequest = onDismissRequest,
    icon = {},
    title = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = stringResource(R.string.loi_name_dialog_title),
          fontSize = 5.em,
        )
      }
    },
    text = {
      Column {
        Text(
          text = stringResource(R.string.loi_name_dialog_body),
        )
        Spacer(Modifier.height(16.dp))
        TextField(
          value = textFieldValue,
          onValueChange = onTextFieldChange,
          colors = textFieldColors,
          singleLine = true,
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = onConfirmRequest,
        colors = saveButtonColors,
        contentPadding = PaddingValues(25.dp, 0.dp),
        enabled = textFieldValue != "",
      ) {
        Text(stringResource(R.string.save))
      }
    },
    dismissButton = {
      OutlinedButton(onClick = { onDismissRequest() }) {
        Text(
          text = stringResource(R.string.cancel),
          color = cancelButtonColor,
        )
      }
    },
    containerColor = colorAttribute(R.attr.colorBackgroundFloating),
    textContentColor = colorAttribute(R.attr.colorOnBackground),
  )
}
