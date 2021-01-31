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

package com.google.android.gnd.system;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.Nullable;

/** Represents the arguments of an {@link Activity#onActivityResult(int, int, Intent)} event. */
public class ActivityResult {

  private final int requestCode;
  private final int resultCode;
  @Nullable private final Intent data;

  public ActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    this.requestCode = requestCode;
    this.resultCode = resultCode;
    this.data = data;
  }

  /** Returns the activity request code for which this result applies. */
  public int getRequestCode() {
    return requestCode;
  }

  /**
   * Returns true iff the system provided a result of {@link Activity#RESULT_OK} to the {@code
   * onActivityResult} callback.
   */
  public boolean isOk() {
    return resultCode == Activity.RESULT_OK;
  }

  /**
   * Returns {@link Intent} data provided by the system to the {@code onActivityResult} callback.
   */
  @Nullable
  public Intent getData() {
    return data;
  }
}
