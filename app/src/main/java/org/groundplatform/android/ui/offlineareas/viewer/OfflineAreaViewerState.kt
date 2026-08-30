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

package org.groundplatform.android.ui.offlineareas.viewer

import androidx.compose.runtime.Immutable
import org.groundplatform.domain.model.imagery.OfflineArea

/** Represents the UI state for the Offline Area Viewer screen. */
@Immutable
data class OfflineAreaViewerState(
  val area: OfflineArea? = null,
  val areaName: String = "",
  val areaSize: String? = null,
  val isProgressOverlayVisible: Boolean = false,
) {
  val isRemoveButtonEnabled: Boolean
    get() = area != null && !isProgressOverlayVisible
}
