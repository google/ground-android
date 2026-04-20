/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.groundplatform.domain.model.map.CameraPosition
import org.groundplatform.domain.model.map.MapType

/** Provides access and storage of persistent map states. */
interface MapStateRepositoryInterface {
  val mapTypeFlow: StateFlow<MapType>
  var mapType: MapType
  val offlineImageryEnabledFlow: StateFlow<Boolean>
  var isOfflineImageryEnabled: Boolean
  var isLocationLockEnabled: Boolean
  fun setCameraPosition(cameraPosition: CameraPosition)
  fun getCameraPosition(surveyId: String): CameraPosition?
}
