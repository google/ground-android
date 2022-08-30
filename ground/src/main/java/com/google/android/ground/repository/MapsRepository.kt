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
package com.google.android.ground.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.ground.persistence.local.LocalValueStore
import javax.inject.Inject
import javax.inject.Singleton

/** Coordinates persistence and retrieval of map type from local value store. */
@Singleton
class MapsRepository @Inject constructor(private val localValueStore: LocalValueStore) {

  private val mutableMapType: MutableLiveData<Int> = MutableLiveData(mapType)

  fun observableMapType(): LiveData<Int> = mutableMapType

  var mapType: Int
    get() = localValueStore.lastMapType
    set(value) {
      localValueStore.lastMapType = value
      mutableMapType.postValue(value)
    }
}
