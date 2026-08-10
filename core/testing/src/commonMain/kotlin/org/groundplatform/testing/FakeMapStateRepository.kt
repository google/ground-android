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
package org.groundplatform.testing

import kotlinx.coroutines.flow.MutableStateFlow
import org.groundplatform.domain.model.map.CameraPosition
import org.groundplatform.domain.model.map.MapType
import org.groundplatform.domain.repository.MapStateRepositoryInterface

class FakeMapStateRepository : MapStateRepositoryInterface {
  var activeSurveyId = ""
  private val cameraPositions = mutableMapOf<String, CameraPosition>()

  override val mapTypeFlow = MutableStateFlow(MapType.ROAD)
  override var mapType: MapType by mapTypeFlow::value

  override val offlineImageryEnabledFlow = MutableStateFlow(true)
  override var isOfflineImageryEnabled: Boolean by offlineImageryEnabledFlow::value

  override var isLocationLockEnabled = false

  override fun setCameraPosition(cameraPosition: CameraPosition) {
    cameraPositions[activeSurveyId] = cameraPosition
  }

  override fun getCameraPosition(surveyId: String) = cameraPositions[surveyId]

  override fun clearCameraPosition(surveyId: String) {
    cameraPositions.remove(surveyId)
  }
}
