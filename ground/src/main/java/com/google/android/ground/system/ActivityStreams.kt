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
package com.google.android.ground.system

import android.app.Activity
import android.content.Intent
import com.google.android.ground.coroutines.ApplicationScope
import java8.util.function.Consumer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Bridge between the [Activity] and various `Manager` classes. */
@Singleton
class ActivityStreams @Inject constructor(@ApplicationScope private val scope: CoroutineScope) {

  /** Emits [Consumer]s to be executed in the context of the [Activity]. */
  private val _activityRequestsFlow: MutableSharedFlow<Consumer<Activity>> = MutableSharedFlow()
  val activityRequests: SharedFlow<Consumer<Activity>> = _activityRequestsFlow.asSharedFlow()

  /** Emits [Activity.onActivityResult] events. */
  private val _activityResults: MutableSharedFlow<ActivityResult> = MutableSharedFlow()

  /** Emits [Activity.onRequestPermissionsResult] events. */
  private val _requestPermissionsResults: MutableSharedFlow<RequestPermissionsResult> =
    MutableSharedFlow()

  /**
   * Queues the specified [Consumer] for execution. An instance of the current [ ] is provided to
   * the `Consumer` when called.
   */
  fun withActivity(callback: Consumer<Activity>) {
    scope.launch { _activityRequestsFlow.emit(callback) }
  }

  /**
   * Callback used to communicate [Activity.onActivityResult] events with various `Manager` classes.
   */
  fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    scope.launch { _activityResults.emit(ActivityResult(requestCode, resultCode, data)) }
  }

  /**
   * Callback used to communicate [Activity.onRequestPermissionsResult] events with various
   * `Manager` classes.
   */
  fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    scope.launch {
      _requestPermissionsResults.emit(
        RequestPermissionsResult(requestCode, permissions, grantResults)
      )
    }
  }

  /** Emits [Activity.onActivityResult] events where `requestCode` matches the specified value. */
  fun getActivityResults(requestCode: Int): Flow<ActivityResult> =
    _activityResults.filter { it.requestCode == requestCode }

  /**
   * Emits the next [Activity.onActivityResult] event where `requestCode` matches the specified
   * value.
   */
  suspend fun getNextActivityResult(requestCode: Int): ActivityResult =
    getActivityResults(requestCode).first() // TODO(#723): Define and handle timeouts.

  /**
   * Emits the next [Activity.onRequestPermissionsResult] event where `requestCode` matches the
   * specified value.
   */
  suspend fun getNextRequestPermissionsResult(requestCode: Int): RequestPermissionsResult =
    _requestPermissionsResults
      .filter { it.requestCode == requestCode }
      .first() // TODO(#723): Define and handle timeouts.
}
