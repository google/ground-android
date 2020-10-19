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

package com.google.android.gnd.rx;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import com.uber.autodispose.AutoDispose;
import com.uber.autodispose.AutoDisposeConverter;
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider;

/**
 * Utility methods providing syntactic sugar for the {@link com.uber.autodispose.AutoDispose}
 * library.
 */
public abstract class RxAutoDispose {
  /** Do not instantiate. */
  private RxAutoDispose() {}

  @NonNull
  public static <T> AutoDisposeConverter<T> autoDisposable(@NonNull final LifecycleOwner lifecycleOwner) {
    return AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(lifecycleOwner));
  }

  @NonNull
  public static <T> AutoDisposeConverter<T> disposeOnDestroy(@NonNull final LifecycleOwner lifecycleOwner) {
    return AutoDispose.autoDisposable(
        AndroidLifecycleScopeProvider.from(lifecycleOwner, Lifecycle.Event.ON_DESTROY));
  }
}
