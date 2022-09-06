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
import com.google.android.ground.rx.annotations.Hot
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

/**
 * Responsible for abstracting navigation from fragment to fragment. Exposes various actions to
 * ViewModels that cause a NavDirections to be emitted to the observer (in this case, the [ ], which
 * is expected to pass it to the current [ ].
 */
@Singleton
class Navigator @Inject constructor() {
  private val navigateRequests: @Hot Subject<NavDirections> = PublishSubject.create()
  private val navigateUpRequests: @Hot Subject<Any> = PublishSubject.create()

  /** Stream of navigation requests for fulfillment by the view layer. */
  fun getNavigateRequests(): Observable<NavDirections> {
    return navigateRequests
  }

  fun getNavigateUpRequests(): Observable<Any> {
    return navigateUpRequests
  }

  /** Navigates up one level on the back stack. */
  fun navigateUp() {
    navigateUpRequests.onNext(Any())
  }

  fun navigate(directions: NavDirections) {
    CoroutineScope(Main).launch { navigateRequests.onNext(directions) }
  }
}
