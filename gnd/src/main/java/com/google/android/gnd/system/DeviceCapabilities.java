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

package com.google.android.gnd.system;

import android.os.Build;

/** Reports various device capabilities based on Android OS version. */
public final class DeviceCapabilities {
  /** Do not instantiate. */
  private DeviceCapabilities() {}

  /**
   * Returns true if the device supports window insets. Insets allow the UI to fill the full screen,
   * including behind the translucent top System Bar and bottom Navigation Bar. Used to determine
   * whether relevant UI elements need to be repositioned to above/below the insets.
   */
  public static boolean isWindowInsetsSupported() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH;
  }

  public static boolean isGenerateViewIdSupported() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
  }
}
