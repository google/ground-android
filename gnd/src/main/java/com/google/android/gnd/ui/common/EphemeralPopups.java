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

package com.google.android.gnd.ui.common;

import android.content.Context;
import android.widget.Toast;
import com.google.android.gnd.R;

public class EphemeralPopups {
  /** Do not instantiate */
  private EphemeralPopups() {}

  public static void showError(Context context, int messageId) {
    Toast.makeText(context, messageId, Toast.LENGTH_LONG).show();
  }

  public static void showError(Context context) {
    showError(context, R.string.unexpected_error);
  }
}
