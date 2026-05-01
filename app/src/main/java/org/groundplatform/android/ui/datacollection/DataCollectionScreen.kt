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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.viewpager2.widget.ViewPager2
import kotlinx.serialization.json.JsonObject
import org.groundplatform.android.R
import org.groundplatform.android.ui.components.ConfirmationDialog
import org.groundplatform.android.ui.components.Toolbar
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.locationofinterest.LoiReport
import org.groundplatform.ui.theme.AppTheme

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
  onExitConfirmed: () -> Unit,
) {
  val footerVerticalPosition by viewModel.footerVerticalPosition.collectAsStateWithLifecycle()
  val showExitWarningDialog by viewModel.showExitWarning.collectAsStateWithLifecycle()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  DataCollectionContent(
    uiState = uiState,
    footerVerticalPosition = footerVerticalPosition,
    onCloseClicked = {
      if (uiState is DataCollectionUiState.TaskSubmitted) {
        onExitConfirmed()
      } else {
        viewModel.showExitWarning()
      }
    },
  ) {
    DataCollectionViewPager(uiState, fragment)
  }

  if (showExitWarningDialog) {
    ConfirmationDialog(
      title = R.string.data_collection_cancellation_title,
      description = R.string.data_collection_cancellation_description,
      confirmButtonText = R.string.data_collection_cancellation_confirm_button,
      onConfirmClicked = {
        viewModel.dismissExitWarning()
        onExitConfirmed()
      },
      onDismiss = { viewModel.dismissExitWarning() },
    )
  }
}

/**
 * Hosts the ViewPager2 that renders individual task screens.
 *
 * @param uiState The current UI state.
 * @param fragment The fragment hosting this screen.
 */
@Composable
private fun DataCollectionViewPager(
  uiState: DataCollectionUiState,
  fragment: DataCollectionFragment,
) {
  AndroidView(
    factory = { context ->
      ViewPager2(context).apply {
        isUserInputEnabled = false
        offscreenPageLimit = 1
      }
    },
    update = { viewPager ->
      if (uiState is DataCollectionUiState.Ready) {
        val currentAdapter = viewPager.adapter as? DataCollectionViewPagerAdapter
        if (currentAdapter == null || currentAdapter.tasks != uiState.tasks) {
          viewPager.adapter = fragment.viewPagerAdapterFactory.create(fragment, uiState.tasks)
        }
        viewPager.doOnLayout { viewPager.setCurrentItem(uiState.position.absoluteIndex, false) }
      }
    },
    modifier = Modifier.fillMaxSize(),
  )
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
          }
      ) {
        when (uiState) {
          is DataCollectionUiState.Loading -> {
            CircularProgressIndicator(
              modifier = Modifier.align(Alignment.Center).testTag("loading_indicator")
            )
          }
          is DataCollectionUiState.Error -> {
            Text(
              text = "Error: ${uiState.code}",
              modifier = Modifier.align(Alignment.Center).testTag("error_message"),
              color = MaterialTheme.colorScheme.error,
            )
          }
          is DataCollectionUiState.TaskSubmitted -> {
            DataSubmissionConfirmationScreen(loiReport = uiState.loiReport) { onCloseClicked() }
          }
          else -> {
            pagerContent()
            if (uiState is DataCollectionUiState.Ready) {
              DataCollectionProgressBar(
                uiState.position,
                progressPositionY = footerVerticalPosition - boxPositionY,
              )
            }
          }
        }
      }
    }
  }
}

/**
 * The toolbar for data collection, displaying title and subtitle based on state.
 *
 * @param uiState The current UI state.
 * @param onCloseClicked Callback when the close button is clicked.
 */
@Composable
private fun DataCollectionToolbar(uiState: DataCollectionUiState, onCloseClicked: () -> Unit) {
  val title =
    when (uiState) {
      is DataCollectionUiState.TaskSubmitted -> stringResource(R.string.data_collection_complete)
      is DataCollectionUiState.Ready -> uiState.job.name ?: ""
      else -> ""
    }
  val subtitle =
    when (uiState) {
      is DataCollectionUiState.Ready -> uiState.loiName
      else -> null
    }

  Toolbar(
    title = title,
    subtitle = subtitle,
    modifier = Modifier.testTag("data_collection_toolbar"),
    navigationIcon = {
      IconButton(onClick = onCloseClicked) {
        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
      }
    },
  )
}

/**
 * Renders the progress bar with dynamic offset based on footer position.
 *
 * @param position Current task position.
 * @param progressPositionY The vertical position of the linear progress bar.
 */
@Composable
private fun DataCollectionProgressBar(position: TaskPosition, progressPositionY: Float) {
  val targetProgress = position.relativeIndex.toFloat() / (position.sequenceSize - 1)
  val progress by animateFloatAsState(targetValue = targetProgress)

  LinearProgressIndicator(
    progress = { progress },
    modifier =
      Modifier.fillMaxWidth()
        .offset { IntOffset(0, progressPositionY.toInt()) }
        .testTag("progress_bar"),
  )
}

@Preview(showBackground = true)
@Composable
private fun DataCollectionContentLoadingPreview() {
  AppTheme {
    DataCollectionContent(
      uiState = DataCollectionUiState.Loading,
      footerVerticalPosition = 100f,
      onCloseClicked = {},
    ) {
      Box(modifier = Modifier.fillMaxSize().background(Color.LightGray)) {
        Text(text = "Pager Content Area", modifier = Modifier.align(Alignment.Center))
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun DataCollectionContentErrorPreview() {
  AppTheme {
    DataCollectionContent(
      uiState =
        DataCollectionUiState.Error(
          code = DataCollectionErrorCode.NO_VALID_TASKS,
          cause = Error("Some error"),
        ),
      footerVerticalPosition = 100f,
      onCloseClicked = {},
    ) {
      Box(modifier = Modifier.fillMaxSize().background(Color.LightGray)) {
        Text(text = "Pager Content Area", modifier = Modifier.align(Alignment.Center))
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun DataCollectionContentPreview() {
  AppTheme {
    DataCollectionContent(
      uiState =
        DataCollectionUiState.Ready(
          surveyId = "survey1",
          job = Job(id = "job1", name = "Test Job"),
          loiName = "Test LOI",
          tasks = emptyList(),
          isAddLoiFlow = false,
          currentTaskId = "task1",
          position = TaskPosition(0, 1, 2),
        ),
      footerVerticalPosition = 2000f,
      onCloseClicked = {},
    ) {
      Box(modifier = Modifier.fillMaxSize().background(Color.LightGray)) {
        Text(text = "Pager Content Area", modifier = Modifier.align(Alignment.Center))
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun DataCollectionContentCompletePreview() {
  AppTheme {
    DataCollectionContent(
      uiState =
        DataCollectionUiState.TaskSubmitted(
          loiReport = LoiReport(loiName = "Point A", geoJson = JsonObject(mapOf()))
        ),
      footerVerticalPosition = 2000f,
      onCloseClicked = {},
    ) {
      Box(modifier = Modifier.fillMaxSize().background(Color.LightGray)) {
        Text(text = "Pager Content Area", modifier = Modifier.align(Alignment.Center))
      }
    }
  }
}
