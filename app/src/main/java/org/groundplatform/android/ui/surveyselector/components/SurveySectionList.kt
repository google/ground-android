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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.groundplatform.android.R
import org.groundplatform.android.model.SurveyListItem
import org.groundplatform.android.proto.Survey
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.components.ConfirmationDialog

/** Renders the content of the survey list, including sections and confirmation dialogs. */
@Composable
fun SurveySectionList(
  sectionData: List<Pair<Int, List<SurveyListItem>>>,
  onConfirmDelete: (String) -> Unit,
  onCardClick: (String) -> Unit,
) {
  val expandedStates = rememberExpandedStates()
  var surveyIdToDelete by remember { mutableStateOf<String?>(null) }

  if (surveyIdToDelete != null) {
    ConfirmationDialog(
      title = R.string.remove_offline_access_warning_title,
      description = R.string.remove_offline_access_warning_dialog_body,
      confirmButtonText = R.string.remove_offline_access_warning_confirm_button,
      onConfirmClicked = {
        surveyIdToDelete?.let(onConfirmDelete)
        surveyIdToDelete = null
      },
      onDismiss = { surveyIdToDelete = null },
    )
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    surveySections(
      sectionData = sectionData,
      expandedStates = expandedStates,
      onCardClick = onCardClick,
      onMenuClick = { surveyId -> surveyIdToDelete = surveyId },
    )
  }
}

/**
 * A [LazyListScope] extension function that builds the survey sections, including headers and
 * survey cards.
 */
private fun LazyListScope.surveySections(
  sectionData: List<Pair<Int, List<SurveyListItem>>>,
  expandedStates: MutableMap<Int, Boolean>,
  onCardClick: (String) -> Unit,
  onMenuClick: (String) -> Unit,
) {
  sectionData.forEach { (titleResId, list) ->
    item(key = titleResId) {
      SectionHeader(
        title = formatSectionTitle(stringResource(id = titleResId), list.size),
        expanded = expandedStates[titleResId] ?: false,
        isClickable = list.isNotEmpty(),
        onClick = {
          if (list.isNotEmpty()) {
            expandedStates[titleResId] = !(expandedStates[titleResId] ?: false)
          }
        },
      )
    }

    if (expandedStates[titleResId] == true) {
      items(list, key = { it.id }) { item ->
        SurveyCardItem(
          item = item,
          onCardClick = { onCardClick(item.id) },
          menuClick = { onMenuClick(item.id) },
        )
      }
    }

    item { Spacer(modifier = Modifier.height(8.dp)) }
  }

  item { Spacer(modifier = Modifier.height(16.dp)) }
}

@Composable
private fun rememberExpandedStates(): MutableMap<Int, Boolean> = remember {
  mutableStateMapOf(
    R.string.section_on_device to true,
    R.string.section_shared_with_me to false,
    R.string.section_public to false,
  )
}

fun formatSectionTitle(title: String, count: Int): String = "$title ($count)"

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
private fun PreviewSurveySectionList() {
  val dummySurveys =
    listOf(
      SurveyListItem("1", "Tree Survey", "Track tree growth", true, Survey.GeneralAccess.PUBLIC),
      SurveyListItem(
        "2",
        "Water Survey",
        "Check water quality",
        false,
        Survey.GeneralAccess.RESTRICTED,
      ),
    )

  val sectionData =
    listOf(
      R.string.section_on_device to dummySurveys,
      R.string.section_shared_with_me to emptyList<SurveyListItem>(),
      R.string.section_public to dummySurveys,
    )

  SurveySectionList(sectionData = sectionData, onConfirmDelete = {}, onCardClick = {})
}
