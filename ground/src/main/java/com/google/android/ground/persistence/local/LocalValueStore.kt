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
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.MapType
import com.google.android.ground.ui.settings.Keys
import com.google.android.ground.util.allowThreadDiskReads
import com.google.android.ground.util.allowThreadDiskWrites
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Simple value store persisted locally on device. Unlike [LocalDataStore], this class provides a
 * concrete implementation using the Android SDK, and therefore does not require a database-specific
 * implementation.
 */
@Singleton
class LocalValueStore @Inject constructor(private val preferences: SharedPreferences) {
  private val mapTypeProcessor: BehaviorProcessor<MapType> =
    BehaviorProcessor.createDefault(mapType)

  private val _offlineImageryEnabled = MutableStateFlow(isOfflineImageryEnabled)

  val mapTypeFlowable: Flowable<MapType>
    get() = allowThreadDiskReads { mapTypeProcessor }

  val offlineImageryEnabledFlow: StateFlow<Boolean> = _offlineImageryEnabled.asStateFlow()

  // TODO: Store tile sources in local db instead of in value store.
  var defaultTileSourceUrl: String
    get() = allowThreadDiskReads {
      preferences.getString(DEFAULT_TILE_SOURCE_URL_KEY, "").orEmpty()
    }
    set(id) = allowThreadDiskWrites {
      preferences.edit().putString(DEFAULT_TILE_SOURCE_URL_KEY, id).apply()
    }

  /**
   * Id of the last survey successfully activated by the user. This value is only updated after the
   * survey activation process is complete.
   */
  var lastActiveSurveyId: String
    // TODO(#1592): Stop using this field to identify current survey.
    get() = allowThreadDiskReads { preferences.getString(ACTIVE_SURVEY_ID_KEY, "").orEmpty() }
    set(id) = allowThreadDiskWrites {
      preferences.edit().putString(ACTIVE_SURVEY_ID_KEY, id).apply()
    }

  /** The last map type selected. */
  var mapType: MapType
    get() = allowThreadDiskReads {
      val mapTypeIdx = preferences.getInt(MAP_TYPE, MapType.DEFAULT.ordinal)
      MapType.values()[mapTypeIdx]
    }
    set(value) = allowThreadDiskWrites {
      preferences.edit().putInt(MAP_TYPE, value.ordinal).apply()
      mapTypeProcessor.onNext(value)
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
      _offlineImageryEnabled.value = value
    }

  /** Removes all values stored in the local store. */
  fun clear() = allowThreadDiskWrites { preferences.edit().clear().apply() }

  fun shouldUploadMediaOverUnmeteredConnectionOnly(): Boolean = allowThreadDiskReads {
    preferences.getBoolean(Keys.UPLOAD_MEDIA, false)
  }

  // TODO(#1964): Consider cleaning up this preference if there are no plans to use it anywhere.
  fun shouldDownloadOfflineAreasOverUnmeteredConnectionOnly(): Boolean = allowThreadDiskReads {
    preferences.getBoolean(Keys.OFFLINE_AREAS, false)
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

  companion object {
    const val ACTIVE_SURVEY_ID_KEY = "activeSurveyId"
    const val MAP_TYPE = "map_type"
    const val LAST_VIEWPORT_PREFIX = "last_viewport_"
    const val TOS_ACCEPTED = "tos_accepted"
    const val LOCATION_LOCK_ENABLED = "location_lock_enabled"
    const val OFFLINE_MAP_IMAGERY = "offline_map_imagery"
    const val DEFAULT_TILE_SOURCE_URL_KEY = "tile_source_url"
  }
}
