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
package org.groundplatform.android.ui.surveyselector.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.SurveyListItem
import org.groundplatform.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JoinSurveyDialog(
  surveyListItem: SurveyListItem,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
) {
  BasicAlertDialog(onDismissRequest = onDismiss) {
    Surface(
      shape = MaterialTheme.shapes.extraLarge,
      color = MaterialTheme.colorScheme.surfaceContainer,
      tonalElevation = 6.dp,
    ) {
      Column(modifier = Modifier.padding(24.dp)) {
        Text(
          text = stringResource(R.string.join_survey_confirm_title),
          style = MaterialTheme.typography.headlineSmall,
          modifier = Modifier.padding(bottom = 16.dp),
        )
        Column(
          modifier =
            Modifier.weight(weight = 1f, fill = false).verticalScroll(rememberScrollState())
        ) {
          SurveyCardItem(item = surveyListItem, descriptionMaxLines = Int.MAX_VALUE)
        }
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.cancel)) }
          TextButton(onClick = onConfirm) { Text(text = stringResource(R.string.ok)) }
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun JoinSurveyDialogPreview() {
  AppTheme {
    Column(modifier = Modifier.fillMaxSize()) {
      JoinSurveyDialog(
        surveyListItem =
          SurveyListItem(
            "1",
            "Tree Survey",
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod " +
              "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis " +
              "nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis " +
              "aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat " +
              "nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui " +
              "officia deserunt mollit anim id est laborum.",
            false,
            Survey.GeneralAccess.UNLISTED,
          ),
        onDismiss = {},
        onConfirm = {},
      )
    }
  }
}
