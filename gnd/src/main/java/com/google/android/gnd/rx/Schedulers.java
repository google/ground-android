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

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Injectable provider of thread schedulers used by applications. These are injected instead of
 * being used statically to allow mocking in unit tests.
 */
@Singleton
public class Schedulers {

  @Inject
  public Schedulers() {}

  public Scheduler io() {
    return io.reactivex.schedulers.Schedulers.io();
  }

  public Scheduler ui() {
    return AndroidSchedulers.mainThread();
  }
}
