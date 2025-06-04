/*
 * Copyright 2018 Google LLC
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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.groundplatform.android.R
import org.groundplatform.android.databinding.SurveySelectorFragBinding
import org.groundplatform.android.model.SurveyListItem
import org.groundplatform.android.proto.Survey.GeneralAccess
import org.groundplatform.android.ui.common.AbstractFragment
import org.groundplatform.android.ui.common.BackPressListener
import org.groundplatform.android.ui.common.EphemeralPopups
import org.groundplatform.android.ui.compose.ConfirmationDialog
import org.groundplatform.android.ui.home.HomeScreenFragmentDirections
import org.groundplatform.android.ui.theme.AppTheme
import org.groundplatform.android.util.visibleIf

/** User interface implementation of survey selector screen. */
@AndroidEntryPoint
class SurveySelectorFragment : AbstractFragment(), BackPressListener {

  @Inject lateinit var ephemeralPopups: EphemeralPopups
  private lateinit var viewModel: SurveySelectorViewModel
  private lateinit var binding: SurveySelectorFragBinding
  private lateinit var adapter: SurveyListAdapter

  private val args: SurveySelectorFragmentArgs by navArgs()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(SurveySelectorViewModel::class.java)
    adapter = SurveyListAdapter(viewModel, this)
    viewModel.uiState.launchWhenStartedAndCollect { updateUi(it) }
    if (!args.surveyId.isNullOrBlank()) {
      viewModel.activateSurvey(args.surveyId!!)
    }
  }

  private fun updateUi(uiState: UiState) {
    when (uiState) {
      UiState.ActivatingSurvey,
      UiState.FetchingSurveys -> {
        showProgressDialog()
      }
      UiState.SurveyActivated -> {
        if (!viewModel.surveyActivationInProgress) {
          dismissProgressDialog()
        }
      }
      is UiState.SurveyListAvailable -> {
        handleSurveyListUpdated(uiState.surveys)
        if (!viewModel.surveyActivationInProgress) {
          dismissProgressDialog()
        }
      }
      is UiState.Error -> {
        dismissProgressDialog()
        ephemeralPopups.ErrorPopup().unknownError()
      }
      is UiState.NavigateToHome -> {
        findNavController().navigate(HomeScreenFragmentDirections.showHomeScreen())
      }
    }
  }

  private fun handleSurveyListUpdated(surveys: List<SurveyListItem>) {
    with(binding) {
      container.visibleIf(surveys.isNotEmpty())
      emptyContainer.visibleIf(surveys.isEmpty())
    }
    binding.composeView.setContent { AppTheme { SurveyList(surveys) } }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    binding = SurveySelectorFragBinding.inflate(inflater, container, false)
    binding.viewModel = viewModel
    binding.lifecycleOwner = this
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    getAbstractActivity().setSupportActionBar(binding.toolbar)

    if (parentFragmentManager.backStackEntryCount > 0) {
      getAbstractActivity().supportActionBar?.setDisplayHomeAsUpEnabled(true)
    } else {
      getAbstractActivity().supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }
  }

  @Composable
  private fun SurveyList(surveys: List<SurveyListItem>) {
    val showDialogState = remember { mutableStateOf(false) }
    var surveyId by remember { mutableStateOf("") }

    val onDevice =
      surveys.filter { it.availableOffline || it.generalAccess == GeneralAccess.RESTRICTED }
    val sharedWith =
      surveys.filter { it.generalAccess == GeneralAccess.UNLISTED && !it.availableOffline }
    val publicList = surveys.filter { it.generalAccess == GeneralAccess.PUBLIC }

    ConfirmationDialog(
      title = R.string.remove_offline_access_warning_title,
      description = R.string.remove_offline_access_warning_dialog_body,
      confirmButtonText = R.string.remove_offline_access_warning_confirm_button,
      onConfirmClicked = { viewModel.deleteSurvey(surveyId) },
      visibleState = showDialogState,
    )

    val sectionData =
      listOf(
        R.string.section_on_device to onDevice,
        R.string.section_shared_with_me to sharedWith,
        R.string.section_public to publicList,
      )

    val expandedStates = remember {
      mutableStateMapOf(
        R.string.section_on_device to true,
        R.string.section_shared_with_me to false,
        R.string.section_public to false,
      )
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      sectionData.forEach { (titleResId, list) ->
        item {
          SectionHeader(
            title = stringResource(id = titleResId),
            count = list.size,
            expanded = expandedStates[titleResId] == true,
            isClickable = list.isNotEmpty(),
            onClick = {
              if (list.isNotEmpty()) {
                expandedStates[titleResId] = !(expandedStates[titleResId] ?: false)
              }
            },
          )
        }

        if (expandedStates[titleResId] == true) {
          SurveyItemExpandedList(
            items = list,
            onActivate = { viewModel.activateSurvey(it) },
            onPopUpClick = { id ->
              surveyId = id
              showDialogState.value = true
            },
          )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }

  private fun LazyListScope.SurveyItemExpandedList(
    items: List<SurveyListItem>,
    onActivate: (String) -> Unit,
    onPopUpClick: (String) -> Unit,
  ) {
    this.items(items, key = { it.id }) { item ->
      SurveyCardItem(
        item = item,
        onActivate = { onActivate(it) },
        onPopUpClick = { onPopUpClick(it) },
      )
    }
  }

  @Composable
  private fun SurveyCardItem(
    item: SurveyListItem,
    modifier: Modifier = Modifier,
    onActivate: (String) -> Unit,
    onPopUpClick: (String) -> Unit,
  ) {
    Card(
      modifier = modifier.fillMaxWidth().clickable { onActivate(item.id) },
      shape = MaterialTheme.shapes.medium,
      colors =
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
        HeaderRow(item) { onPopUpClick(item.id) }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = item.title,
          fontFamily = FontFamily(Font(R.font.text_500)),
          lineHeight = 28.sp,
          fontSize = 18.sp,
          fontWeight = FontWeight(500),
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = item.description,
          fontSize = 14.sp,
          lineHeight = 20.sp,
          fontWeight = FontWeight(400),
          fontFamily = FontFamily(Font(R.font.text_500)),
          color = MaterialTheme.colorScheme.outline,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }

  @Composable
  private fun HeaderRow(item: SurveyListItem, onPopUpClick: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Icon(
        painter =
          painterResource(
            when (item.generalAccess.ordinal) {
              2 -> R.drawable.ic_unlisted
              3 -> R.drawable.ic_public
              else -> R.drawable.ic_restricted
            }
          ),
        contentDescription = stringResource(R.string.offline_icon_description),
        modifier = Modifier.size(24.dp).padding(end = 4.dp),
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text =
          when (item.generalAccess.ordinal) {
            2 -> stringResource(R.string.access_unlisted)
            3 -> stringResource(R.string.access_public)
            else -> stringResource(R.string.access_restricted)
          },
        fontFamily = FontFamily(Font(R.font.text_500)),
        lineHeight = 16.sp,
        fontSize = 12.sp,
        fontWeight = FontWeight(500),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.weight(1f))
      if (item.availableOffline) {
        Icon(
          tint = Color(0xff006E2C),
          painter = painterResource(R.drawable.ic_offline_pin),
          contentDescription = stringResource(R.string.offline_icon_description),
          modifier = Modifier.size(24.dp).padding(end = 4.dp),
        )
        Icon(
          painter = painterResource(R.drawable.ic_more_vert),
          contentDescription = "",
          modifier = Modifier.size(24.dp).padding(end = 4.dp).clickable { onPopUpClick(item.id) },
        )
      }
    }
  }

  @Composable
  private fun SectionHeader(
    title: String,
    count: Int,
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
        text = "$title ($count)",
        fontFamily = FontFamily(Font(R.font.text_500)),
        lineHeight = 16.sp,
        fontSize = 16.sp,
        fontWeight = FontWeight(500),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }

  private fun shouldExitApp(): Boolean =
    arguments?.let { SurveySelectorFragmentArgs.fromBundle(it).shouldExitApp } ?: false

  override fun onBack(): Boolean {
    if (shouldExitApp()) {
      requireActivity().finish()
      return true
    }
    return false
  }
}
