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

import java8.util.function.Consumer;

/**
 * Wrapper for events passed through streams that should be handled at most once. This is used
 * to prevent events that trigger dialogs or other notifications from retriggering when views are
 * restored on configuration change.
 *
 * @param <T> The event data.
 */
public class Event<T> {
  private final T value;
  private boolean handled;

  private Event(T value) {
    this.value = value;
    this.handled = false;
  }

  /**
   * Invokes the provided consumer if the value has not yet been handled.
   *
   * @param consumer
   */
  public synchronized void ifUnhandled(Consumer<T> consumer) {
    if (!handled) {
      this.handled = true;
      consumer.accept(value);
    }
  }

  /**
   * Returns a new event wrapping the specified value.
   *
   * @param value
   * @param <T>
   */
  public static <T> Event<T> of(T value) {
    return new Event<>(value);
  }
}
