/*
 * Copyright 2019 Google LLC
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
package com.google.android.ground.rx

import java8.util.function.Consumer

/**
 * Wrapper for events passed through streams that should be handled at most once. This is used to
 * prevent events that trigger dialogs or other notifications from re-triggering when views are
 * restored on configuration change.
 *
 * @param <T> The event data. </T>
 */
class Event<T> private constructor(private val data: T) : Action() {

  /** Invokes the provided consumer if the value has not yet been handled. */
  @Synchronized
  fun ifUnhandled(consumer: Consumer<T>) {
    ifUnhandled(Runnable { consumer.accept(data) })
  }

  companion object {
    /** Returns a new event with the specified event data. */
    @JvmStatic
    fun <T> create(data: T): Event<T> {
      return Event(data)
    }
  }
}
