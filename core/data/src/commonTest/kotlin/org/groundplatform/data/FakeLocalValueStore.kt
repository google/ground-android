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
package org.groundplatform.data

import org.groundplatform.data.stores.LocalValueStore
import org.groundplatform.domain.model.map.CameraPosition
import org.groundplatform.domain.util.Constants.DEFAULT_MAP_TYPE

class FakeLocalValueStore : LocalValueStore {
  override var lastActiveSurveyId: String = ""
  override var mapType: Int = DEFAULT_MAP_TYPE.ordinal
  override var isLocationLockEnabled: Boolean = false
  override var isTermsOfServiceAccepted: Boolean = false
  override var isOfflineImageryEnabled: Boolean = true
  override var drawAreaInstructionsShown: Boolean = false
  override var dropPinInstructionsShown: Boolean = false
  override var draftSubmissionId: String? = null
  override var selectedLanguage: String = "en"
  override var selectedLengthUnit: String = "m"
  override var shouldUploadMediaOverUnmeteredConnectionOnly: Boolean = false
  override var isDeferredDeeplinkConsumed: Boolean = false

  private val cameraPositions = mutableMapOf<String, CameraPosition>()
  private val dataSharingConsents = mutableMapOf<String, Boolean>()

  override fun clear() {
    lastActiveSurveyId = ""
    mapType = DEFAULT_MAP_TYPE.ordinal
    isLocationLockEnabled = false
    isTermsOfServiceAccepted = false
    isOfflineImageryEnabled = true
    drawAreaInstructionsShown = false
    dropPinInstructionsShown = false
    draftSubmissionId = null
    selectedLanguage = "en"
    selectedLengthUnit = "m"
    shouldUploadMediaOverUnmeteredConnectionOnly = false
    isDeferredDeeplinkConsumed = false
    cameraPositions.clear()
    dataSharingConsents.clear()
  }

  override fun clearLastCameraPosition(surveyId: String) {
    cameraPositions.remove(surveyId)
  }

  override fun setLastCameraPosition(surveyId: String, cameraPosition: CameraPosition) {
    cameraPositions[surveyId] = cameraPosition
  }

  override fun getLastCameraPosition(surveyId: String): CameraPosition? = cameraPositions[surveyId]

  override fun setDataSharingConsent(surveyId: String, consent: Boolean) {
    dataSharingConsents[surveyId] = consent
  }

  override fun getDataSharingConsent(surveyId: String): Boolean =
    dataSharingConsents[surveyId] ?: false
}
