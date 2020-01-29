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

/** The result of an operation that can return either true, false, or fail with an exception. */
public class BooleanOrError extends ValueOrError<Boolean> {
  private BooleanOrError(@Nullable Boolean value, @Nullable Throwable error) {
    super(value, error);
  }

  public static BooleanOrError ofTrue() {
    return new BooleanOrError(true, null);
  }

  public static BooleanOrError ofFalse() {
    return new BooleanOrError(false, null);
  }

  public static BooleanOrError error(Throwable t) {
    return new BooleanOrError(null, t);
  }

  /**
   * Returns true if the operation succeeded with a result of {@code true}, or false otherwise. Note
   * that false is also returned if the operation failed in error.
   */
  public boolean isTrue() {
    return value().orElse(false);
  }
}
