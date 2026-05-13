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
package org.groundplatform.android.ui.datacollection

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.ui.components.ConfirmationDialog

/**
 * The main screen for data collection, coordinating the task sequence and host UI.
 *
 * @param viewModel The view model for data collection.
 * @param fragment The fragment hosting this screen.
 * @param onExitConfirmed Callback when the user confirms exiting the data collection flow.
 */
@Composable
fun DataCollectionScreen(
  viewModel: DataCollectionViewModel,
  fragment: DataCollectionFragment,
  onValidationError: (resId: Int) -> Unit,
  onExitConfirmed: () -> Unit,
) {
  val showExitWarningDialog by viewModel.showExitWarning.collectAsStateWithLifecycle()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(Unit) {
    viewModel.uiEffects.collect { effect ->
      when (effect) {
        is DataCollectionUiEffect.Exit -> onExitConfirmed()
        is DataCollectionUiEffect.ShowValidationError -> onValidationError(effect.errorResId)
      }
    }
  }

  DataCollectionContent(uiState = uiState, onCloseClicked = { viewModel.onCloseClicked() }) {
    readyState ->
    val tasks = readyState.tasks
    val position = readyState.position
    val currentTask = tasks[position.relativeIndex]

    key(currentTask.id) { TaskScreenContainer(currentTask, position, fragment) }
  }

  if (showExitWarningDialog) {
    ConfirmationDialog(
      title = R.string.data_collection_cancellation_title,
      description = R.string.data_collection_cancellation_description,
      confirmButtonText = R.string.data_collection_cancellation_confirm_button,
      onConfirmClicked = { viewModel.confirmExit() },
      onDismiss = { viewModel.dismissExitWarning() },
    )
  }
}

@VisibleForTesting
object DataCollectionScreenTestTags {
  const val TOOLBAR = "data_collection_toolbar"
  const val LOADING_INDICATOR = "loading_indicator"
  const val ERROR_MESSAGE = "error_message"
}

/**
 * The layout content of the data collection screen, including toolbar, pager content, and progress
 * bar.
 *
 * @param uiState The current UI state.
 * @param onCloseClicked Callback when the close button is clicked.
 * @param pagerContent The content Composable for the pager area.
 */
@Composable
fun DataCollectionContent(
  uiState: DataCollectionUiState,
  onCloseClicked: () -> Unit,
  pagerContent: @Composable (DataCollectionUiState.Ready) -> Unit,
) {
  Scaffold(topBar = { DataCollectionToolbar(uiState, onCloseClicked) }) { innerPadding ->
    Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
      Box(
        modifier = Modifier.weight(1f).align(Alignment.CenterHorizontally),
        contentAlignment = Alignment.Center,
      ) {
        when (uiState) {
          is DataCollectionUiState.Loading -> {
            LoadingContent()
          }
          is DataCollectionUiState.Error -> {
            ErrorContent()
          }
          is DataCollectionUiState.Ready -> {
            ReadyContent { pagerContent(uiState) }
          }
          is DataCollectionUiState.TaskSubmitted -> {
            DataSubmissionConfirmationScreen(loiReport = uiState.loiReport) { onCloseClicked() }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataCollectionToolbar(uiState: DataCollectionUiState, onCloseClicked: () -> Unit) {
  CenterAlignedTopAppBar(
    modifier = Modifier.testTag(DataCollectionScreenTestTags.TOOLBAR),
    title = {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = uiState.getTitle(), textAlign = TextAlign.Center)
        val subtitle = uiState.getSubtitle()
        if (!subtitle.isNullOrBlank()) {
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
          )
        }
      }
    },
    navigationIcon = {
      IconButton(onClick = onCloseClicked) {
        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
      }
    },
  )
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
  CircularProgressIndicator(
    modifier = modifier.testTag(DataCollectionScreenTestTags.LOADING_INDICATOR)
  )
}

@Composable
private fun ErrorContent(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.testTag(DataCollectionScreenTestTags.ERROR_MESSAGE),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(text = stringResource(R.string.unexpected_error), color = MaterialTheme.colorScheme.error)
  }
}

@Composable
private fun ReadyContent(pagerContent: @Composable () -> Unit) {
  pagerContent()
}

@Composable
private fun DataCollectionUiState.getTitle(): String =
  when (this) {
    is DataCollectionUiState.TaskSubmitted -> stringResource(R.string.data_collection_complete)
    is DataCollectionUiState.Ready -> job.name ?: ""
    else -> ""
  }

@Composable
private fun DataCollectionUiState.getSubtitle(): String? =
  if (this is DataCollectionUiState.Ready) loiName else null
