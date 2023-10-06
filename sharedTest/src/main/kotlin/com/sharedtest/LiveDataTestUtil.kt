/*
 * Copyright 2022 Google LLC
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

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@VisibleForTesting(otherwise = VisibleForTesting.NONE)
fun <T> LiveData<T>.getOrAwaitValue(
  time: Long = 2,
  timeUnit: TimeUnit = TimeUnit.SECONDS,
  afterObserve: () -> Unit = {}
): T {
  var data: T? = null
  val latch = CountDownLatch(1)
  val observer =
    object : Observer<T> {
      override fun onChanged(value: T) {
        data = value
        latch.countDown()
        this@getOrAwaitValue.removeObserver(this)
      }
    }
  this.observeForever(observer)

  try {
    afterObserve.invoke()

    // Don't wait indefinitely if the LiveData is not set.
    if (!latch.await(time, timeUnit)) {
      throw TimeoutException("LiveData value was never set.")
    }
  } finally {
    this.removeObserver(observer)
  }

  return data!!
}
