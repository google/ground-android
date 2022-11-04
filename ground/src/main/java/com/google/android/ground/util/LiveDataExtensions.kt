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
package com.google.android.ground.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

/** Combines a LiveData<T> with a LiveData<K> and returns a LiveData<R> */
fun <T, K, R> LiveData<T>.combineWith(liveData: LiveData<K>, block: (T?, K?) -> R): LiveData<R> {
  val result = MediatorLiveData<R>()
  result.addSource(this) { result.value = block(this.value, liveData.value) }
  result.addSource(liveData) { result.value = block(this.value, liveData.value) }
  return result
}
