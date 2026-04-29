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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.viewpager2.widget.ViewPager2
import org.groundplatform.android.R
import org.groundplatform.android.ui.components.ConfirmationDialog
import org.groundplatform.android.ui.components.Toolbar
import org.groundplatform.android.ui.main.MainViewModel
import org.groundplatform.android.util.systemInsets
import org.groundplatform.domain.model.job.Job
import org.groundplatform.ui.theme.AppTheme

@Composable
fun DataCollectionScreen(
  viewModel: DataCollectionViewModel,
  mainViewModel: MainViewModel,
  fragment: DataCollectionFragment,
  onExitConfirmed: () -> Unit,
) {
  val footerVerticalPosition by viewModel.footerVerticalPosition.collectAsStateWithLifecycle()
  val showExitWarningDialog by viewModel.showExitWarning.collectAsStateWithLifecycle()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val windowInsets by
    mainViewModel.windowInsets.asFlow().collectAsStateWithLifecycle(initialValue = null)

  DataCollectionContent(
    uiState = uiState,
    footerVerticalPosition = footerVerticalPosition,
    windowInsets = windowInsets,
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

@Composable
fun DataCollectionContent(
  uiState: DataCollectionUiState,
  footerVerticalPosition: Float,
  windowInsets: WindowInsetsCompat?,
  onCloseClicked: () -> Unit,
  pagerContent: @Composable () -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize()) {
    DataCollectionToolbar(uiState, onCloseClicked)

    Box(modifier = Modifier.weight(1f)) {
      pagerContent()
      DataCollectionProgressBar(uiState, footerVerticalPosition, windowInsets)
    }
  }
}

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

@Composable
private fun DataCollectionProgressBar(
  uiState: DataCollectionUiState,
  footerVerticalPosition: Float,
  windowInsets: WindowInsetsCompat?,
) {
  if (uiState is DataCollectionUiState.Ready) {
    val progress = uiState.position.relativeIndex.toFloat() / (uiState.position.sequenceSize - 1)
    val topInset = windowInsets?.systemInsets()?.top ?: 0
    val offset = footerVerticalPosition - topInset

    LinearProgressIndicator(
      progress = { progress },
      modifier =
        Modifier.fillMaxWidth().offset { IntOffset(0, offset.toInt()) }.testTag("progress_bar"),
    )
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
      footerVerticalPosition = 100f,
      windowInsets = null,
      onCloseClicked = {},
    ) {
      Box(modifier = Modifier.fillMaxSize().background(Color.LightGray)) {
        Text(text = "Pager Content Area", modifier = Modifier.align(Alignment.Center))
      }
    }
  }
}
