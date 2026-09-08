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
package org.groundplatform.android.ui.offlineareas.selector

import androidx.compose.runtime.Immutable

/**
 * Represents the complete UI state of the Offline Area Selector screen.
 *
 * @property bottomTextState Describes what should be displayed in the bottom text area. This may be
 *   null when no message should be shown.
 * @property downloadState Represents the current state of the download operation, whether a
 *   download is in progress and its progress.
 */
@Immutable
data class OfflineAreaSelectorState(
  val bottomTextState: BottomTextState? = null,
  val downloadState: DownloadState = DownloadState.Idle,
) {
  val isDownloadButtonEnabled: Boolean
    get() = bottomTextState is BottomTextState.AreaSize && downloadState is DownloadState.Idle

  sealed class BottomTextState {
    data object Loading : BottomTextState()

    data class AreaSize(val size: String) : BottomTextState()

    data object NoImageryAvailable : BottomTextState()

    data object AreaTooLarge : BottomTextState()

    data object NetworkError : BottomTextState()
  }

  sealed class DownloadState {
    data object Idle : DownloadState()

    data class InProgress(val progress: Float) : DownloadState()
  }
}
