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
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Responsible for abstracting navigation from fragment to fragment. Exposes various actions to
 * ViewModels that cause a NavDirections to be emitted to the flow.
 */
@Singleton
class Navigator @Inject constructor() {
  private val navigateRequests: MutableStateFlow<NavDirections?> = MutableStateFlow(null)
  private val navigateUpRequests: MutableStateFlow<Any?> = MutableStateFlow(null)
  private val finishRequests: MutableStateFlow<Any?> = MutableStateFlow(null)

  /** Stream of navigation requests for fulfillment by the view layer. */
  fun getNavigateRequests(): Flow<NavDirections> = navigateRequests.filterNotNull()

  fun getNavigateUpRequests(): Flow<Any> = navigateUpRequests.filterNotNull()

  fun getFinishRequests(): Flow<Any> = finishRequests.filterNotNull()

  /** Navigates up one level on the back stack. */
  fun navigateUp() {
    CoroutineScope(Main).launch { navigateUpRequests.emit(Any()) }
  }

  fun navigate(directions: NavDirections) {
    CoroutineScope(Main).launch { navigateRequests.emit(directions) }
  }

  fun finishApp() {
    CoroutineScope(Main).launch { finishRequests.emit(Any()) }
  }
}
