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
import com.google.android.ground.rx.annotations.Hot
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java8.util.function.Consumer
import javax.inject.Inject
import javax.inject.Singleton

/** Bridge between the [Activity] and various `Manager` classes. */
@Singleton
class ActivityStreams @Inject constructor() {

  /** Emits [Consumer]s to be executed in the context of the [Activity]. */
  val activityRequests: @Hot Subject<Consumer<Activity>> = PublishSubject.create()

  /** Emits [Activity.onActivityResult] events. */
  private val activityResults: @Hot Subject<ActivityResult> = PublishSubject.create()

  /** Emits [Activity.onRequestPermissionsResult] events. */
  private val requestPermissionsResults: @Hot Subject<RequestPermissionsResult> =
    PublishSubject.create()

  /**
   * Queues the specified [Consumer] for execution. An instance of the current [ ] is provided to
   * the `Consumer` when called.
   */
  fun withActivity(callback: Consumer<Activity>) = activityRequests.onNext(callback)

  /**
   * Callback used to communicate [Activity.onActivityResult] events with various `Manager` classes.
   */
  fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) =
    activityResults.onNext(ActivityResult(requestCode, resultCode, data))

  /**
   * Callback used to communicate [Activity.onRequestPermissionsResult] events with various
   * `Manager` classes.
   */
  fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) =
    requestPermissionsResults.onNext(
      RequestPermissionsResult(requestCode, permissions, grantResults)
    )

  /** Emits [Activity.onActivityResult] events where `requestCode` matches the specified value. */
  fun getActivityResults(requestCode: Int): @Hot Observable<ActivityResult> =
    activityResults.filter { it.requestCode == requestCode }

  /**
   * Emits the next [Activity.onActivityResult] event where `requestCode` matches the specified
   * value.
   */
  fun getNextActivityResult(requestCode: Int): @Hot Observable<ActivityResult> =
    getActivityResults(requestCode).take(1) // TODO(#723): Define and handle timeouts.

  /**
   * Emits the next [Activity.onRequestPermissionsResult] event where `requestCode` matches the
   * specified value.
   */
  fun getNextRequestPermissionsResult(requestCode: Int): Observable<RequestPermissionsResult> =
    requestPermissionsResults
      .filter { it.requestCode == requestCode }
      .take(1) // TODO(#723): Define and handle timeouts.
}
