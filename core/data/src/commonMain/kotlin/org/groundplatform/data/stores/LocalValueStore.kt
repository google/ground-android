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
package org.groundplatform.data.stores

import org.groundplatform.domain.model.map.CameraPosition

/** Interface for storing key-value pairs locally on device. */
interface LocalValueStore {
  /**
   * Id of the last survey successfully activated by the user. This value is only updated after the
   * survey activation process is complete.
   */
  var lastActiveSurveyId: String

  /** The last map type selected. */
  var mapType: Int

  /** Whether location lock is enabled or not. */
  var isLocationLockEnabled: Boolean

  /** Terms of service acceptance state for the currently signed in user. */
  var isTermsOfServiceAccepted: Boolean

  /** Whether to overlay offline map imagery. */
  var isOfflineImageryEnabled: Boolean

  /** Whether to display instructions when loading a draw area task. */
  var drawAreaInstructionsShown: Boolean

  /** Whether to display instructions when loading a drop pin task. */
  var dropPinInstructionsShown: Boolean

  /** Id of the active draft submission, or null if none. */
  var draftSubmissionId: String?

  /** The currently selected UI language tag. */
  var selectedLanguage: String

  /** The preferred measurement unit system (e.g. Metric or Imperial). */
  var selectedLengthUnit: String

  /** Whether media uploads should only occur over unmetered (e.g. Wi-Fi) connections. */
  var shouldUploadMediaOverUnmeteredConnectionOnly: Boolean

  /** Whether a deferred deep link has already been handled. */
  var isDeferredDeeplinkConsumed: Boolean

  /** Removes all values stored in the local store. */
  fun clear()

  /** Clears the last saved camera position for the given [surveyId]. */
  fun clearLastCameraPosition(surveyId: String)

  /** Saves the last [cameraPosition] viewed for the given [surveyId]. */
  fun setLastCameraPosition(surveyId: String, cameraPosition: CameraPosition)

  /** Returns the last saved camera position for the given [surveyId], or null if none was saved. */
  fun getLastCameraPosition(surveyId: String): CameraPosition?

  /** Sets the user's data sharing consent for the given [surveyId]. */
  fun setDataSharingConsent(surveyId: String, consent: Boolean)

  /** Returns whether data sharing consent was granted for the given [surveyId]. */
  fun getDataSharingConsent(surveyId: String): Boolean
}
