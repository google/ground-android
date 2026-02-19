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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.ui.components.LoadingDialog
import org.groundplatform.android.ui.components.Toolbar
import org.groundplatform.android.ui.surveyselector.components.SurveyEmptyState
import org.groundplatform.android.ui.surveyselector.components.SurveySectionList

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
  onError: (Throwable) -> Unit,
  viewModel: SurveySelectorViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  // Collect and handle one-shot events in a LaunchedEffect
  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is SurveySelectorEvent.NavigateToHome -> onNavigateToHomeScreen()
        is SurveySelectorEvent.ShowError -> onError(event.error)
      }
    }
  }

  SurveySelectorScreen(
    uiState = uiState,
    onBack = onBack,
    onSignOut = viewModel::signOut,
    onConfirmDelete = viewModel::confirmDelete,
    onCardClick = viewModel::activateSurvey,
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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurveySelectorScreen(
  uiState: SurveySelectorUiState,
  onBack: () -> Unit,
  onSignOut: () -> Unit,
  onConfirmDelete: (String) -> Unit,
  onCardClick: (String) -> Unit,
) {
  Scaffold(
    topBar = {
      Toolbar(stringRes = R.string.surveys, showNavigationIcon = true, iconClick = onBack)
    }
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
