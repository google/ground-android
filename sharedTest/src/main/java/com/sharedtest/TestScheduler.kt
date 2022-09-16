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
import io.reactivex.schedulers.Schedulers as RxSchedulers
import javax.inject.Inject

/**
 * Runs all tasks synchronously by executing the tasks on the current thread without any queueing
 * and blocking the call until finished.
 */
class TestScheduler @Inject internal constructor() : Schedulers {

  override fun io(): Scheduler = createCurrentThreadScheduler("Test I/O Scheduler")
  override fun ui(): Scheduler = createCurrentThreadScheduler("Test UI Scheduler")

  private fun createCurrentThreadScheduler(name: String): Scheduler {
    val scheduler = RxSchedulers.trampoline()
    val wrapped = Rx2Idler.wrap(scheduler, name)
    IdlingRegistry.getInstance().register(wrapped)
    return wrapped
  }
}
