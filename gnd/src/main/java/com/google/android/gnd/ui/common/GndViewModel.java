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

import android.arch.lifecycle.ViewModel;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * Base class for ViewModels.
 */
public class GndViewModel extends ViewModel {
  private CompositeDisposable subscriptions = new CompositeDisposable();

  /**
   * Causes the provided subscription to be disposed when the ViewModel is about to be destroyed.
   * This is necessary to prevent memory leaks due to subscriptions that never completed holding
   * references to the ViewModel and related objects. This include Single, Maybe, and Completable,
   * which like other Rx streams, are not guaranteed to complete.
   */
  protected void autoDispose(Disposable subscription) {
    subscriptions.add(subscription);
  }

  @Override
  protected void onCleared() {
    subscriptions.clear();
  }
}
