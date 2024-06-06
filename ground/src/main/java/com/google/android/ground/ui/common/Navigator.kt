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
package com.google.android.ground.ui.common

import androidx.navigation.NavDirections
import com.google.android.ground.coroutines.MainDispatcher
import com.google.android.ground.coroutines.MainScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

/**
 * Responsible for abstracting navigation from fragment to fragment. Exposes various actions to
 * ViewModels that cause a NavDirections to be emitted to the observer (in this case, the
 * [com.google.android.ground.MainActivity], which is expected to pass it to the current
 * [androidx.navigation.NavController].
 */
@Singleton
class Navigator
@Inject
constructor(
  @MainDispatcher private val dispatcher: CoroutineDispatcher,
  @MainScope private val coroutineScope: CoroutineScope,
) {
  private val _navigateRequests = MutableSharedFlow<NavigationRequest>()

  /** Stream of navigation requests for fulfillment by the view layer. */
  fun getNavigateRequests(): SharedFlow<NavigationRequest> =
    _navigateRequests.shareIn(coroutineScope, SharingStarted.Lazily, replay = 0)

  /** Navigates up one level on the back stack. */
  fun navigateUp() {
    requestNavigation(NavigateUp)
  }

  fun navigate(directions: NavDirections) {
    requestNavigation(NavigateTo(directions))
  }

  fun finishApp() {
    requestNavigation(FinishApp)
  }

  private fun requestNavigation(request: NavigationRequest) {
    CoroutineScope(dispatcher).launch { _navigateRequests.emit(request) }
  }
}
