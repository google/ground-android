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

package com.google.android.gnd.rx;

import javax.annotation.Nullable;

/**
 * The result of an asynchronous enable/disable operation.
 */
public class EnableState extends OperationState<Boolean> {
  private EnableState(boolean enabled, @Nullable Throwable error) {
    super(enabled, error);
  }

  public static EnableState enabled() {
    return new EnableState(true, null);
  }

  public static EnableState disabled() {
    return new EnableState(false, null);
  }

  public static EnableState error(Throwable t) {
    return new EnableState(false, t);
  }

  public boolean isEnabled() {
    return get();
  }

  @Override
  public String toString() {
    return error().map(t -> "error: " + t).orElse(get() ? "enabled" : "disabled");
  }
}
