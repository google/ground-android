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
package org.groundplatform.android.ui.datacollection.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import org.groundplatform.android.R

const val LOI_NAME_TEXT_FIELD_TEST_TAG: String = "loi name text field test tag"

@Composable
fun LoiNameDialog(
  textFieldValue: String,
  onConfirmRequest: () -> Unit,
  onDismissRequest: () -> Unit,
  onTextFieldChange: (String) -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismissRequest,
    title = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.loi_name_dialog_title), fontSize = 5.em)
      }
    },
    text = {
      Column {
        Text(text = stringResource(R.string.loi_name_dialog_body))
        Spacer(Modifier.height(16.dp))
        TextField(
          value = textFieldValue,
          onValueChange = onTextFieldChange,
          singleLine = true,
          modifier = Modifier.testTag(LOI_NAME_TEXT_FIELD_TEST_TAG),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onConfirmRequest,
        contentPadding = PaddingValues(25.dp, 0.dp),
        enabled = textFieldValue != "",
      ) {
        Text(stringResource(R.string.save))
      }
    },
    dismissButton = {
      OutlinedButton(onClick = { onDismissRequest() }) {
        Text(text = stringResource(R.string.cancel))
      }
    },
  )
}
