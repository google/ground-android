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

import android.support.annotation.NonNull;
import java8.util.Optional;

/**
 * Represents an event that should be consumed and processed only once.
 *
 * Based on "LiveData with SnackBar, Navigation and other events (the SingleLiveEvent case)":
 * https://medium.com/google-developers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150
 */
public class Consumable<T> {
  private boolean consumed;
  private T content;

  public Consumable(@NonNull T content) {
    this.content = content;
  }

  public boolean isConsumed() {
    return consumed;
  }

  /**
   * Returns the content on first invocation, returns empty Optional thereafter.
   */
  public Optional<T> get() {
    if (consumed) {
      return Optional.empty();
    } else {
      consumed = true;
      return Optional.of(content);
    }
  }

  /**
   * Returns the content, regardless of whether it's already been consumed.
   */
  public T peek() {
    return content;
  }
}
