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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.components.LoadingDialog
import org.groundplatform.android.ui.components.Toolbar
import org.groundplatform.android.ui.surveyselector.components.SurveyEmptyState
import org.groundplatform.android.ui.surveyselector.components.SurveySectionList
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.SurveyListItem
import org.groundplatform.ui.theme.AppTheme

/**
 * Stateful composable that handles ViewModel interactions and side effects for the Survey Selector
 * screen.
 *
 * @param onBack Callback when the back button is pressed.
 * @param onNavigateToHomeScreen Callback when a survey is selected and the user should be directed
 *   to the home screen.
 * @param onError Callback to handle and display errors encountered during survey selection.
 * @param viewModel The ViewModel for this screen.
 */
@Composable
fun SurveySelectorScreen(
  onBack: () -> Unit,
  onNavigateToHomeScreen: () -> Unit,
  onError: (SurveySelectorEvent.ErrorType) -> Unit,
  viewModel: SurveySelectorViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  // Collect and handle one-shot events in a LaunchedEffect
  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is SurveySelectorEvent.NavigateToHome -> onNavigateToHomeScreen()
        is SurveySelectorEvent.ShowError -> onError(event.errorType)
      }
    }
  }

  SurveySelectorScreen(
    uiState = uiState,
    onBack = onBack,
    onSignOut = viewModel::signOut,
    onConfirmDelete = viewModel::confirmDelete,
    onCardClick = viewModel::activateSurvey,
    onScanQrCode = viewModel::scanQrCodeAndActivateSurvey,
  )
}

/**
 * Stateless Survey Selector screen that renders the UI based on the provided state.
 *
 * @param uiState The state to render.
 * @param onBack Callback when the back button is pressed.
 * @param onSignOut Callback when the user attempts to sign out from the empty state.
 * @param onConfirmDelete Callback when a local survey deletion is confirmed.
 * @param onCardClick Callback when a survey card is clicked to activate it.
 * @param onScanQrCode Callback when the user taps the scan-QR action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurveySelectorScreen(
  uiState: SurveySelectorUiState,
  onBack: () -> Unit,
  onSignOut: () -> Unit,
  onConfirmDelete: (String) -> Unit,
  onCardClick: (String) -> Unit,
  onScanQrCode: () -> Unit,
) {
  Scaffold(
    topBar = {
      Toolbar(stringRes = R.string.surveys, showNavigationIcon = true, iconClick = onBack)
    },
    floatingActionButton = {
      if (!uiState.isLoading) {
        ExtendedFloatingActionButton(
          onClick = onScanQrCode,
          icon = {
            Icon(
              painter = painterResource(id = R.drawable.ic_qr_code_scanner),
              contentDescription = null,
              tint = Color.Black,
            )
          },
          text = { Text(stringResource(R.string.join_survey)) },
        )
      }
    },
  ) { innerPadding ->
    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
      when {
        uiState.showEmptyState -> {
          SurveyEmptyState(onSignOut = onSignOut)
        }
        uiState.hasSurveys -> {
          SurveySectionList(
            sectionData = uiState.surveySections,
            onConfirmDelete = onConfirmDelete,
            onCardClick = onCardClick,
          )
        }
      }

      if (uiState.isLoading) {
        LoadingDialog(R.string.loading)
      }
    }
  }
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
@ExcludeFromJacocoGeneratedReport
private fun PreviewSurveySelectorScreenEmpty() {
  AppTheme {
    SurveySelectorScreen(
      uiState = SurveySelectorUiState(),
      onBack = {},
      onSignOut = {},
      onConfirmDelete = {},
      onCardClick = {},
      onScanQrCode = {},
    )
  }
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
@ExcludeFromJacocoGeneratedReport
private fun PreviewSurveySelectorScreenWithSurveys() {
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

  val uiState =
    SurveySelectorUiState(
      onDeviceSurveys = dummySurveys,
      sharedSurveys = emptyList(),
      publicSurveys = dummySurveys,
    )

  AppTheme {
    SurveySelectorScreen(
      uiState = uiState,
      onBack = {},
      onSignOut = {},
      onConfirmDelete = {},
      onCardClick = {},
      onScanQrCode = {},
    )
  }
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
@ExcludeFromJacocoGeneratedReport
private fun PreviewSurveySelectorScreenLoading() {
  AppTheme {
    SurveySelectorScreen(
      uiState = SurveySelectorUiState(isLoading = true),
      onBack = {},
      onSignOut = {},
      onConfirmDelete = {},
      onCardClick = {},
      onScanQrCode = {},
    )
  }
}
