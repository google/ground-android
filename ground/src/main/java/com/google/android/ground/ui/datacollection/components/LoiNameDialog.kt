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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.google.android.ground.R
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.material.color.MaterialColors

private fun AbstractFragment.getColor(id: Int): Color =
  Color(ContextCompat.getColor(requireContext(), id))

private fun AbstractFragment.getMaterialColor(id: Int): Color =
  Color(MaterialColors.getColor(requireContext(), id, ""))

@Composable
private fun AbstractFragment.getElementColors():
  Triple<ButtonColors, Color, TextFieldColors> {
  val primaryColor = getMaterialColor(R.attr.colorPrimary)
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
  val cancelButtonColor = getMaterialColor(R.attr.colorPrimary)
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
  fragment: AbstractFragment,
  textFieldValue: String,
  onConfirmRequest: () -> Unit,
  onDismissRequest: () -> Unit,
  onTextFieldChange: (String) -> Unit
) {
  val (saveButtonColors, cancelButtonColor, textFieldColors) = fragment.getElementColors()
  AlertDialog(
    onDismissRequest = onDismissRequest,
    icon = {},
    title = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = fragment.getString(R.string.loi_name_dialog_title),
          fontSize = 5.em,
          textAlign = TextAlign.Start,
        )
      }
    },
    text = {
      Column {
        Text(
          text = fragment.getString(R.string.loi_name_dialog_body),
          modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 16.dp),
        )
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
        Text(fragment.getString(R.string.save))
      }
    },
    dismissButton = {
      OutlinedButton(onClick = { onDismissRequest() }) {
          Text(
            text = fragment.getString(R.string.cancel),
            color = cancelButtonColor,
          )
        }
    },
    containerColor = fragment.getColor(R.color.md_theme_background),
    textContentColor = fragment.getColor(R.color.md_theme_onBackground),
  )
}
