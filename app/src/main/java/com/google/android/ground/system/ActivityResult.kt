/*
 * Copyright 2021 Google LLC
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

/** Represents the arguments of an [Activity.onActivityResult] event. */
data class ActivityResult(val requestCode: Int, val resultCode: Int, val data: Intent?) {

  /**
   * Returns true iff the system provided a result of [Activity.RESULT_OK] to the `onActivityResult`
   * callback.
   */
  fun isOk() = resultCode == Activity.RESULT_OK
}
