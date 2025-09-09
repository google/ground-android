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
package org.groundplatform.android.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.model.map.CameraPosition
import org.groundplatform.android.model.map.MapType

/** Provides access and storage of persistent map states. */
@Singleton
class MapStateRepository @Inject constructor(private val localValueStore: LocalValueStore) {

  private val _mapType = MutableStateFlow(mapType)
  val mapTypeFlow: StateFlow<MapType> = _mapType.asStateFlow()
  var mapType: MapType
    get() = MapType.entries[localValueStore.mapType]
    set(value) {
      localValueStore.mapType = value.ordinal
      _mapType.update { value }
    }

  private val _offlineImageryEnabled = MutableStateFlow(isOfflineImageryEnabled)
  val offlineImageryEnabledFlow: StateFlow<Boolean> = _offlineImageryEnabled.asStateFlow()
  var isOfflineImageryEnabled: Boolean
    get() = localValueStore.isOfflineImageryEnabled
    set(value) {
      localValueStore.isOfflineImageryEnabled = value
      _offlineImageryEnabled.update { value }
    }

  var isLocationLockEnabled: Boolean by localValueStore::isLocationLockEnabled

  fun setCameraPosition(cameraPosition: CameraPosition) =
    localValueStore.setLastCameraPosition(localValueStore.lastActiveSurveyId, cameraPosition)

  fun getCameraPosition(surveyId: String): CameraPosition? =
    localValueStore.getLastCameraPosition(surveyId)
}
