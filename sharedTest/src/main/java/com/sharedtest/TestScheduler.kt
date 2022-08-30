/*
 * Copyright 2020 Google LLC
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
package com.sharedtest

import androidx.test.espresso.IdlingRegistry
import com.google.android.ground.rx.Schedulers
import com.squareup.rx2.idler.Rx2Idler
import io.reactivex.Scheduler
import javax.inject.Inject

/**
 * Runs all tasks synchronously by executing the tasks on the current thread without any queueing
 * and blocking the call until finished.
 */
class TestScheduler @Inject constructor() : Schedulers {

  private fun scheduler() = io.reactivex.schedulers.Schedulers.trampoline()

  override fun io(): Scheduler {
    val wrapped = Rx2Idler.wrap(scheduler(), "Test I/O Scheduler")
    IdlingRegistry.getInstance().register(wrapped)
    return wrapped
  }

  override fun ui(): Scheduler {
    val wrapped = Rx2Idler.wrap(scheduler(), "Test UI Scheduler")
    IdlingRegistry.getInstance().register(wrapped)
    return wrapped
  }
}
