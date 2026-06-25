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
package org.groundplatform.android.ui.home.mapcontainer

import androidx.annotation.StringRes
import org.groundplatform.android.ui.home.mapcontainer.jobs.DataCollectionEntryPointData
import org.groundplatform.domain.model.Survey

/**
 * One-off events emitted by [HomeScreenMapContainerViewModel] for the host fragment to render
 * (popups, navigation, dialogs).
 */
sealed interface HomeScreenMapContainerUiEffect {
  data class ShowError(@StringRes val messageId: Int) : HomeScreenMapContainerUiEffect

  data class ShowInfo(@StringRes val messageId: Int) : HomeScreenMapContainerUiEffect

  data class NavigateToDataCollection(val data: DataCollectionEntryPointData) :
    HomeScreenMapContainerUiEffect

  data class ShowDataSharingTerms(
    val data: DataCollectionEntryPointData,
    val terms: Survey.DataSharingTerms,
  ) : HomeScreenMapContainerUiEffect
}
