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
package org.groundplatform.android.persistence.local

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.groundplatform.android.ui.map.CameraPosition
import org.groundplatform.android.ui.map.MapType
import org.groundplatform.android.ui.settings.Keys
import org.groundplatform.android.util.allowThreadDiskReads
import org.groundplatform.android.util.allowThreadDiskWrites
import timber.log.Timber

/**
 * Simple value store persisted locally on device. Unlike [LocalDataStoreModule], this class
 * provides a concrete implementation using the Android SDK, and therefore does not require a
 * database-specific implementation.
 */
@Singleton
class LocalValueStore @Inject constructor(private val preferences: SharedPreferences) {
  private val _mapType = MutableStateFlow(mapType)
  private val _offlineImageryEnabled = MutableStateFlow(isOfflineImageryEnabled)

  val mapTypeFlow: StateFlow<MapType> = _mapType.asStateFlow()
  val offlineImageryEnabledFlow: StateFlow<Boolean> = _offlineImageryEnabled.asStateFlow()

  /**
   * Id of the last survey successfully activated by the user. This value is only updated after the
   * survey activation process is complete.
   */
  var lastActiveSurveyId: String
    get() = allowThreadDiskReads { preferences.getString(ACTIVE_SURVEY_ID_KEY, "").orEmpty() }
    set(id) = allowThreadDiskWrites {
      preferences.edit().putString(ACTIVE_SURVEY_ID_KEY, id).apply()
    }

  /** The last map type selected. */
  var mapType: MapType
    get() = allowThreadDiskReads {
      val mapTypeIdx = preferences.getInt(MAP_TYPE, MapType.DEFAULT.ordinal)
      MapType.entries[mapTypeIdx]
    }
    set(value) = allowThreadDiskWrites {
      preferences.edit().putInt(MAP_TYPE, value.ordinal).apply()
      _mapType.update { value }
    }

  /** Whether location lock is enabled or not. */
  var isLocationLockEnabled: Boolean
    get() = allowThreadDiskReads { preferences.getBoolean(LOCATION_LOCK_ENABLED, false) }
    set(value) = allowThreadDiskWrites {
      preferences.edit().putBoolean(LOCATION_LOCK_ENABLED, value).apply()
    }

  /** Terms of service acceptance state for the currently signed in user. */
  var isTermsOfServiceAccepted: Boolean
    get() = allowThreadDiskReads { preferences.getBoolean(TOS_ACCEPTED, false) }
    set(value) = allowThreadDiskWrites {
      preferences.edit().putBoolean(TOS_ACCEPTED, value).apply()
    }

  /** Whether to overlay offline map imagery. */
  var isOfflineImageryEnabled: Boolean
    get() = allowThreadDiskReads { preferences.getBoolean(OFFLINE_MAP_IMAGERY, true) }
    set(value) = allowThreadDiskReads {
      preferences.edit().putBoolean(OFFLINE_MAP_IMAGERY, value).apply()
      _offlineImageryEnabled.update { value }
    }

  /** Whether to display instructions when loading a draw area task. */
  var drawAreaInstructionsShown: Boolean
    get() = allowThreadDiskReads { preferences.getBoolean(DRAW_AREA_INSTRUCTIONS_SHOWN, false) }
    set(value) = allowThreadDiskReads {
      preferences.edit().putBoolean(DRAW_AREA_INSTRUCTIONS_SHOWN, value).apply()
    }

  /** Whether to display instructions when loading a drop pin task. */
  var dropPinInstructionsShown: Boolean
    get() = allowThreadDiskReads { preferences.getBoolean(DROP_PIN_INSTRUCTIONS_SHOWN, false) }
    set(value) = allowThreadDiskReads {
      preferences.edit().putBoolean(DROP_PIN_INSTRUCTIONS_SHOWN, value).apply()
    }

  var draftSubmissionId: String?
    get() = allowThreadDiskReads { preferences.getString(DRAFT_SUBMISSION_ID, null) }
    set(value) = allowThreadDiskReads {
      preferences.edit().putString(DRAFT_SUBMISSION_ID, value).apply()
    }

  /** Removes all values stored in the local store. */
  fun clear() = allowThreadDiskWrites { preferences.edit().clear().apply() }

  fun shouldUploadMediaOverUnmeteredConnectionOnly(): Boolean = allowThreadDiskReads {
    preferences.getBoolean(Keys.UPLOAD_MEDIA, false)
  }

  fun setLastCameraPosition(surveyId: String, cameraPosition: CameraPosition) =
    allowThreadDiskReads {
      preferences
        .edit()
        .putString(LAST_VIEWPORT_PREFIX + surveyId, cameraPosition.serialize())
        .apply()
    }

  fun getLastCameraPosition(surveyId: String): CameraPosition? = allowThreadDiskReads {
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
  }

  fun setDataSharingConsent(surveyId: String, consent: Boolean) {
    preferences.edit().putBoolean(DATA_SHARING_CONSENT_PREFIX + surveyId, consent).apply()
  }

  fun getDataSharingConsent(surveyId: String): Boolean = allowThreadDiskReads {
    return preferences.getBoolean(DATA_SHARING_CONSENT_PREFIX + surveyId, false)
  }

  companion object {
    const val ACTIVE_SURVEY_ID_KEY = "activeSurveyId"
    const val MAP_TYPE = "map_type"
    const val LAST_VIEWPORT_PREFIX = "last_viewport_"
    const val TOS_ACCEPTED = "tos_accepted"
    const val LOCATION_LOCK_ENABLED = "location_lock_enabled"
    const val OFFLINE_MAP_IMAGERY = "offline_map_imagery"
    const val DRAW_AREA_INSTRUCTIONS_SHOWN = "draw_area_instructions_shown"
    const val DROP_PIN_INSTRUCTIONS_SHOWN = "drop_pin_instructions_shown"
    const val DRAFT_SUBMISSION_ID = "draft_submission_id"
    const val DATA_SHARING_CONSENT_PREFIX = "data_consent_"
  }
}
