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
import kotlinx.coroutines.flow.Flow
import org.groundplatform.android.persistence.local.LocalValueStore
import org.groundplatform.android.ui.map.CameraPosition
import org.groundplatform.android.ui.map.MapType

/** Provides access and storage of persistent map states. */
@Singleton
class MapStateRepository @Inject constructor(private val localValueStore: LocalValueStore) {

  val mapTypeFlow: Flow<MapType> by localValueStore::mapTypeFlow
  var mapType: MapType by localValueStore::mapType
  var isLocationLockEnabled: Boolean by localValueStore::isLocationLockEnabled
  val offlineImageryEnabledFlow: Flow<Boolean> by localValueStore::offlineImageryEnabledFlow
  var isOfflineImageryEnabled: Boolean by localValueStore::isOfflineImageryEnabled

  fun setCameraPosition(cameraPosition: CameraPosition) =
    localValueStore.setLastCameraPosition(localValueStore.lastActiveSurveyId, cameraPosition)

  fun getCameraPosition(surveyId: String): CameraPosition? =
    localValueStore.getLastCameraPosition(surveyId)
}
