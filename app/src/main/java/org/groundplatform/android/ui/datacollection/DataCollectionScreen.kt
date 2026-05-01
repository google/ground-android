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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.ui.components.ConfirmationDialog
import org.groundplatform.android.ui.components.Toolbar

object DataCollectionScreenTestTags {
  const val TOOLBAR = "data_collection_toolbar"
  const val LOADING_INDICATOR = "loading_indicator"
  const val ERROR_MESSAGE = "error_message"
  const val PROGRESS_BAR = "progress_bar"
}

/**
 * The main screen for data collection, coordinating the task sequence and host UI.
 *
 * @param viewModel The view model for data collection.
 * @param fragment The fragment hosting this screen (retained for ViewPager2 adapter creation).
 * @param onExitConfirmed Callback when the user confirms exiting the data collection flow.
 */
@Composable
fun DataCollectionScreen(
  viewModel: DataCollectionViewModel,
  fragment: DataCollectionFragment,
  onValidationError: (resId: Int) -> Unit,
  onExitConfirmed: () -> Unit,
) {
  val footerVerticalPosition by viewModel.footerVerticalPosition.collectAsStateWithLifecycle()
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

  DataCollectionContent(
    uiState = uiState,
    footerVerticalPosition = footerVerticalPosition,
    onCloseClicked = { viewModel.onCloseClicked() },
  ) {
    DataCollectionViewPager(uiState, fragment)
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

/**
 * The layout content of the data collection screen, including toolbar, pager content, and progress
 * bar.
 *
 * @param uiState The current UI state.
 * @param footerVerticalPosition The vertical position of the footer.
 * @param onCloseClicked Callback when the close button is clicked.
 * @param pagerContent The content Composable for the pager area.
 */
@Composable
fun DataCollectionContent(
  uiState: DataCollectionUiState,
  footerVerticalPosition: Float,
  onCloseClicked: () -> Unit,
  pagerContent: @Composable () -> Unit,
) {
  Scaffold(topBar = { DataCollectionToolbar(uiState, onCloseClicked) }) { innerPadding ->
    Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
      var boxPositionY by remember { mutableFloatStateOf(0f) }

      Box(
        modifier =
          Modifier.weight(1f).align(Alignment.CenterHorizontally).onGloballyPositioned {
            boxPositionY = it.positionInWindow().y
          },
        contentAlignment = Alignment.Center,
      ) {
        when (uiState) {
          is DataCollectionUiState.Loading -> {
            LoadingContent()
          }
          is DataCollectionUiState.Error -> {
            ErrorContent(uiState.code)
          }
          is DataCollectionUiState.Ready -> {
            ReadyContent(
              position = uiState.position,
              footerVerticalPosition = footerVerticalPosition,
              boxPositionY = boxPositionY,
              pagerContent = pagerContent,
            )
          }
          is DataCollectionUiState.TaskSubmitted -> {
            DataSubmissionConfirmationScreen(loiReport = uiState.loiReport) { onCloseClicked() }
          }
        }
      }
    }
  }
}

@Composable
private fun DataCollectionToolbar(uiState: DataCollectionUiState, onCloseClicked: () -> Unit) {
  Toolbar(
    title = uiState.getTitle(),
    subtitle = uiState.getSubtitle(),
    modifier = Modifier.testTag(DataCollectionScreenTestTags.TOOLBAR),
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
private fun ErrorContent(code: DataCollectionErrorCode, modifier: Modifier = Modifier) {
  Text(
    text = "Error: $code",
    modifier = modifier.testTag(DataCollectionScreenTestTags.ERROR_MESSAGE),
    color = MaterialTheme.colorScheme.error,
  )
}

@Composable
private fun BoxScope.ReadyContent(
  position: TaskPosition,
  footerVerticalPosition: Float,
  boxPositionY: Float,
  pagerContent: @Composable () -> Unit,
) {
  pagerContent()
  DataCollectionProgressBar(
    position,
    progressPositionY = footerVerticalPosition - boxPositionY,
    modifier = Modifier.align(Alignment.TopCenter),
  )
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
  when (this) {
    is DataCollectionUiState.Ready -> loiName
    else -> null
  }

@Composable
private fun DataCollectionProgressBar(
  position: TaskPosition,
  progressPositionY: Float,
  modifier: Modifier = Modifier,
) {
  val targetProgress = position.relativeIndex.toFloat() / (position.sequenceSize - 1)
  val progress by animateFloatAsState(targetValue = targetProgress)

  LinearProgressIndicator(
    progress = { progress },
    modifier =
      modifier
        .fillMaxWidth()
        .offset { IntOffset(0, progressPositionY.toInt()) }
        .testTag(DataCollectionScreenTestTags.PROGRESS_BAR),
  )
}
