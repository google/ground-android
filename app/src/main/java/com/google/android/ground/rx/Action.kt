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

/**
 * Wrapper for actions passed through streams that should be handled at most once. This is used to
 * prevent actions that trigger dialogs or other notifications from retriggering when views are
 * restored on configuration change.
 */
open class Action protected constructor() {
  private var handled = false

  /** Invokes the provided handler if the value has not yet been handled. */
  @Synchronized
  fun ifUnhandled(handler: Runnable) {
    if (!handled) {
      handled = true
      handler.run()
    }
  }
}
