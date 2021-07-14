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

package com.google.android.gnd;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

/**
 * Lifecycle {@link Observer} helpers used for testing.
 */
public class TestObservers {

  /**
   * Observes the provided {@link LiveData} until the first value is emitted. This is useful for
   * testing cold LiveData streams, since by definition their upstream observables do not begin
   * emitting items until the stream is observed/subscribed to.
   */
  public static <T> void observeUntilFirstChange(LiveData<T> liveData) {
    liveData.observeForever(
        new Observer<T>() {
          @Override
          public void onChanged(T value) {
            liveData.removeObserver(this);
          }
        });
  }
}
