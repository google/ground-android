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
package org.groundplatform.android.ui.surveyselector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.groundplatform.android.R
import org.groundplatform.android.model.SurveyListItem
import org.groundplatform.android.proto.Survey
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.components.ConfirmationDialog
import org.groundplatform.android.ui.surveyselector.components.SectionHeader
import org.groundplatform.android.ui.surveyselector.components.SurveyCardItem

@Composable
fun SurveyList(surveys: List<SurveyListItem>, viewModel: SurveySelectorViewModel) {
  LaunchedEffect(surveys) { viewModel.setSurveys(surveys) }

  val onDeviceSurveys by viewModel.onDeviceSurveys.collectAsState()
  val sharedWithSurveys by viewModel.sharedWithSurveys.collectAsState()
  val publicListSurveys by viewModel.publicListSurveys.collectAsState()
  val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
  val expandedStates = rememberExpandedStates()

  val sectionData =
    listOf(
      R.string.section_on_device to onDeviceSurveys,
      R.string.section_shared_with_me to sharedWithSurveys,
      R.string.section_public to publicListSurveys,
    )

  SurveyListContent(
    sectionData = sectionData,
    expandedStates = expandedStates,
    showDeleteDialog = showDeleteDialog,
    onConfirmDelete = { viewModel.selectedSurveyId.value?.let { viewModel.confirmDelete(it) } },
    onCardClick = viewModel::activateSurvey,
    onMenuClick = viewModel::openDeleteDialog,
    onDismissDialog = { viewModel.closeDeleteDialog() },
  )
}

@Composable
fun SurveyListContent(
  sectionData: List<Pair<Int, List<SurveyListItem>>>,
  expandedStates: MutableMap<Int, Boolean>,
  showDeleteDialog: Boolean,
  onConfirmDelete: () -> Unit,
  onCardClick: (String) -> Unit,
  onMenuClick: (String) -> Unit,
  onDismissDialog: () -> Unit,
) {
  if (showDeleteDialog) {
    ConfirmationDialog(
      title = R.string.remove_offline_access_warning_title,
      description = R.string.remove_offline_access_warning_dialog_body,
      confirmButtonText = R.string.remove_offline_access_warning_confirm_button,
      onConfirmClicked = onConfirmDelete,
      onDismiss = onDismissDialog,
    )
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
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
        SurveyItemExpandedList(items = list, onCardClick = onCardClick, menuClick = onMenuClick)
      }

      item { Spacer(modifier = Modifier.height(8.dp)) }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@Composable
private fun rememberExpandedStates(): MutableMap<Int, Boolean> = remember {
  mutableStateMapOf(
    R.string.section_on_device to true,
    R.string.section_shared_with_me to false,
    R.string.section_public to false,
  )
}

private fun LazyListScope.SurveyItemExpandedList(
  items: List<SurveyListItem>,
  onCardClick: (String) -> Unit,
  menuClick: (String) -> Unit,
) {
  this.items(items, key = { it.id }) { item ->
    SurveyCardItem(item = item, onCardClick = { onCardClick(it) }, menuClick = { menuClick(it) })
  }
}

fun formatSectionTitle(title: String, count: Int): String = "$title ($count)"

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PreviewSurveyList() {
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
      R.string.section_shared_with_me to emptyList(),
      R.string.section_public to dummySurveys,
    )

  val expandedStates = remember {
    mutableStateMapOf(
      R.string.section_on_device to true,
      R.string.section_shared_with_me to false,
      R.string.section_public to false,
    )
  }

  SurveyListContent(
    sectionData = sectionData,
    expandedStates = expandedStates,
    showDeleteDialog = false,
    onConfirmDelete = {},
    onCardClick = {},
    onMenuClick = {},
    onDismissDialog = {},
  )
}
