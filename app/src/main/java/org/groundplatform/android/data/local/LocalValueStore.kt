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
package org.groundplatform.android.data.local

import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import org.groundplatform.android.common.Constants.DEFAULT_MAP_TYPE
import org.groundplatform.android.common.Constants.LENGTH_UNIT_METER
import org.groundplatform.android.common.PrefKeys
import org.groundplatform.android.model.map.CameraPosition
import org.groundplatform.android.util.allowThreadDiskReads
import org.groundplatform.android.util.allowThreadDiskWrites
import timber.log.Timber

/**
 * Simple value store persisted locally on device. Unlike [LocalDataStoreModule], this class
 * provides a concrete implementation using the Android SDK, and therefore does not require a
 * database-specific implementation.
 */
@Singleton
class LocalValueStore
@Inject
constructor(private val preferences: SharedPreferences, private val locale: Locale) {

  /**
   * Id of the last survey successfully activated by the user. This value is only updated after the
   * survey activation process is complete.
   */
  var lastActiveSurveyId: String
    get() = allowThreadDiskReads {
      preferences.getString(PrefKeys.ACTIVE_SURVEY_ID_KEY, "").orEmpty()
    }
    set(id) = allowThreadDiskWrites {
      preferences.edit { putString(PrefKeys.ACTIVE_SURVEY_ID_KEY, id) }
    }

  /** The last map type selected. */
  var mapType: Int
    get() = allowThreadDiskReads { preferences.getInt(PrefKeys.MAP_TYPE, DEFAULT_MAP_TYPE.ordinal) }
    set(value) = allowThreadDiskWrites { preferences.edit { putInt(PrefKeys.MAP_TYPE, value) } }

  /** Whether location lock is enabled or not. */
  var isLocationLockEnabled: Boolean
    get() = allowThreadDiskReads { preferences.getBoolean(PrefKeys.LOCATION_LOCK_ENABLED, false) }
    set(value) = allowThreadDiskWrites {
      preferences.edit { putBoolean(PrefKeys.LOCATION_LOCK_ENABLED, value) }
    }

  /** Terms of service acceptance state for the currently signed in user. */
  var isTermsOfServiceAccepted: Boolean
    get() = allowThreadDiskReads { preferences.getBoolean(PrefKeys.TOS_ACCEPTED, false) }
    set(value) = allowThreadDiskWrites {
      preferences.edit { putBoolean(PrefKeys.TOS_ACCEPTED, value) }
    }

  /** Whether to overlay offline map imagery. */
  var isOfflineImageryEnabled: Boolean
    get() = allowThreadDiskReads { preferences.getBoolean(PrefKeys.OFFLINE_MAP_IMAGERY, true) }
    set(value) = allowThreadDiskReads {
      preferences.edit { putBoolean(PrefKeys.OFFLINE_MAP_IMAGERY, value) }
    }

  /** Whether to display instructions when loading a draw area task. */
  var drawAreaInstructionsShown: Boolean
    get() = allowThreadDiskReads {
      preferences.getBoolean(PrefKeys.DRAW_AREA_INSTRUCTIONS_SHOWN, false)
    }
    set(value) = allowThreadDiskReads {
      preferences.edit { putBoolean(PrefKeys.DRAW_AREA_INSTRUCTIONS_SHOWN, value) }
    }

  /** Whether to display instructions when loading a drop pin task. */
  var dropPinInstructionsShown: Boolean
    get() = allowThreadDiskReads {
      preferences.getBoolean(PrefKeys.DROP_PIN_INSTRUCTIONS_SHOWN, false)
    }
    set(value) = allowThreadDiskReads {
      preferences.edit { putBoolean(PrefKeys.DROP_PIN_INSTRUCTIONS_SHOWN, value) }
    }

  var draftSubmissionId: String?
    get() = allowThreadDiskReads { preferences.getString(PrefKeys.DRAFT_SUBMISSION_ID, null) }
    set(value) = allowThreadDiskReads {
      preferences.edit { putString(PrefKeys.DRAFT_SUBMISSION_ID, value) }
    }

  var selectedLanguage: String
    get() = allowThreadDiskReads {
      preferences.getString(PrefKeys.LANGUAGE, locale.language) ?: locale.language
    }
    set(value) = allowThreadDiskReads { preferences.edit { putString(PrefKeys.LANGUAGE, value) } }

  var selectedLengthUnit: String
    get() = allowThreadDiskReads {
      preferences.getString(PrefKeys.LENGTH_UNIT, LENGTH_UNIT_METER) ?: LENGTH_UNIT_METER
    }
    set(value) = allowThreadDiskReads {
      preferences.edit { putString(PrefKeys.LENGTH_UNIT, value) }
    }

  /** Removes all values stored in the local store. */
  fun clear() = allowThreadDiskWrites { preferences.edit { clear() } }

  fun shouldUploadMediaOverUnmeteredConnectionOnly(): Boolean = allowThreadDiskReads {
    preferences.getBoolean(PrefKeys.UPLOAD_MEDIA, false)
  }

  fun clearLastCameraPosition(surveyId: String) = allowThreadDiskReads {
    preferences.edit { remove(PrefKeys.LAST_VIEWPORT_PREFIX + surveyId) }
  }

  fun setLastCameraPosition(surveyId: String, cameraPosition: CameraPosition) =
    allowThreadDiskReads {
      preferences.edit {
        putString(PrefKeys.LAST_VIEWPORT_PREFIX + surveyId, cameraPosition.serialize())
      }
    }

  fun getLastCameraPosition(surveyId: String): CameraPosition? = allowThreadDiskReads {
    try {
      val stringVal = preferences.getString(PrefKeys.LAST_VIEWPORT_PREFIX + surveyId, "").orEmpty()
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
    preferences.edit { putBoolean(PrefKeys.DATA_SHARING_CONSENT_PREFIX + surveyId, consent) }
  }

  fun getDataSharingConsent(surveyId: String): Boolean = allowThreadDiskReads {
    return preferences.getBoolean(PrefKeys.DATA_SHARING_CONSENT_PREFIX + surveyId, false)
  }
}
