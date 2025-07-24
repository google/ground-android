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

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.groundplatform.android.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.R
import org.groundplatform.android.model.SurveyListItem
import org.groundplatform.android.proto.Survey
import org.groundplatform.android.ui.compose.ConfirmationDialog

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
) {
  if (showDeleteDialog) {
    ConfirmationDialog(
      title = R.string.remove_offline_access_warning_title,
      description = R.string.remove_offline_access_warning_dialog_body,
      confirmButtonText = R.string.remove_offline_access_warning_confirm_button,
      onConfirmClicked = onConfirmDelete,
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

private fun Survey.GeneralAccess.iconRes(): Int =
  when (ordinal) {
    Survey.GeneralAccess.UNLISTED.ordinal -> R.drawable.ic_unlisted
    Survey.GeneralAccess.PUBLIC.ordinal -> R.drawable.ic_public
    else -> R.drawable.ic_restricted
  }

private fun Survey.GeneralAccess.labelString(): Int =
  when (ordinal) {
    Survey.GeneralAccess.UNLISTED.ordinal -> R.string.access_unlisted
    Survey.GeneralAccess.PUBLIC.ordinal -> R.string.access_public
    else -> R.string.access_restricted
  }

@Composable
private fun SurveyCardItem(
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

@Composable
private fun HeaderRow(item: SurveyListItem, menuClick: (String) -> Unit) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Icon(
      painter = painterResource(item.generalAccess.iconRes()),
      contentDescription = stringResource(R.string.offline_icon_description),
      modifier = Modifier.size(24.dp).padding(end = 4.dp),
    )

    Text(
      text = stringResource(item.generalAccess.labelString()),
      fontFamily = FontFamily(Font(R.font.text_500)),
      fontSize = 12.sp,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(start = 4.dp),
    )

    Spacer(modifier = Modifier.weight(1f))

    if (item.availableOffline) {
      Icon(
        painter = painterResource(R.drawable.ic_offline_pin),
        contentDescription = stringResource(R.string.offline_icon_description),
        tint = Color(0xff006E2C),
        modifier = Modifier.size(24.dp).padding(end = 4.dp),
      )
      Icon(
        painter = painterResource(R.drawable.ic_more_vert),
        contentDescription = stringResource(R.string.more_options_icon_description),
        modifier = Modifier.size(24.dp).clickable { menuClick(item.id) }.padding(end = 4.dp),
      )
    }
  }
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

@Composable
private fun SectionHeader(
  title: String,
  expanded: Boolean,
  isClickable: Boolean,
  onClick: () -> Unit,
) {
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
  )
}
