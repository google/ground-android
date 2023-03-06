/*
 * Copyright 2019 Google LLC
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
package com.google.android.ground.persistence.local

import android.content.SharedPreferences
import com.google.android.gms.maps.GoogleMap
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.settings.Keys
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Simple value store persisted locally on device. Unlike [LocalDataStore], this class provides a
 * concrete implementation using the Android SDK, and therefore does not require a database-specific
 * implementation.
 */
@Singleton
class LocalValueStore @Inject constructor(private val preferences: SharedPreferences) {
  private val mapTypeProcessor: BehaviorProcessor<Int> = BehaviorProcessor.createDefault(mapType)

  val mapTypeFlowable: Flowable<Int>
    get() = mapTypeProcessor

  /**
   * Id of the last survey successfully activated by the user. This value is only updated after the
   * survey activation process is complete.
   */
  var lastActiveSurveyId: String
    // TODO(#1592): Stop using this field to identify current survey.
    get() = preferences.getString(ACTIVE_SURVEY_ID_KEY, "").orEmpty()
    set(id) = preferences.edit().putString(ACTIVE_SURVEY_ID_KEY, id).apply()

  /** Id of the basemap type. */
  var mapType: Int
    get() = preferences.getInt(MAP_TYPE, GoogleMap.MAP_TYPE_HYBRID)
    set(type) {
      preferences.edit().putInt(MAP_TYPE, type).apply()
      mapTypeProcessor.onNext(type)
    }

  /** Whether location lock is enabled or not. */
  var isLocationLockEnabled: Boolean
    get() = preferences.getBoolean(LOCATION_LOCK_ENABLED, false)
    set(value) {
      preferences.edit().putBoolean(LOCATION_LOCK_ENABLED, value).apply()
    }

  /** Terms of service acceptance state for the currently signed in user. */
  var isTermsOfServiceAccepted: Boolean
    get() = preferences.getBoolean(TOS_ACCEPTED, false)
    set(value) {
      preferences.edit().putBoolean(TOS_ACCEPTED, value).apply()
    }

  /** Removes all values stored in the local store. */
  fun clear() {
    preferences.edit().clear().apply()
  }

  fun shouldUploadMediaOverUnmeteredConnectionOnly(): Boolean =
    preferences.getBoolean(Keys.UPLOAD_MEDIA, false)

  fun shouldDownloadOfflineAreasOverUnmeteredConnectionOnly(): Boolean =
    preferences.getBoolean(Keys.OFFLINE_AREAS, false)

  fun setLastCameraPosition(surveyId: String, cameraPosition: CameraPosition) {
    preferences
      .edit()
      .putString(LAST_VIEWPORT_PREFIX + surveyId, cameraPosition.serialize())
      .apply()
  }

  fun getLastCameraPosition(surveyId: String): CameraPosition? =
    try {
      val stringVal = preferences.getString(LAST_VIEWPORT_PREFIX + surveyId, "").orEmpty()
      CameraPosition.deserialize(stringVal)
    } catch (e: NumberFormatException) {
      Timber.e(e, "Invalid camera pos in prefs")
      null
    } catch (e: ArrayIndexOutOfBoundsException) {
      Timber.e(e, "Invalid camera pos in prefs")
      null
    }

  companion object {
    const val ACTIVE_SURVEY_ID_KEY = "activeSurveyId"
    const val MAP_TYPE = "map_type"
    const val LAST_VIEWPORT_PREFIX = "last_viewport_"
    const val TOS_ACCEPTED = "tos_accepted"
    const val LOCATION_LOCK_ENABLED = "location_lock_enabled"
  }
}
