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

package com.google.android.gnd.persistence.local;

import static java8.util.J8Arrays.stream;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.ui.map.CameraPosition;
import com.google.android.gnd.ui.settings.Keys;
import java8.util.Optional;
import java8.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

/**
 * Simple value store persisted locally on device. Unlike {@link LocalDataStore}, this class
 * provides a concrete implementation using the Android SDK, and therefore does not require a
 * database-specific implementation.
 */
@Singleton
public class LocalValueStore {

  public static final String ACTIVE_PROJECT_ID_KEY = "activeProjectId";
  public static final String MAP_TYPE = "map_type";
  public static final String LAST_VIEWPORT_PREFIX = "last_viewport_";
  public static final String TOS_ACCEPTED = "tos_accepted";
  public static final String POLYGON_INFO_DIALOG = "polygon_info_dialog";

  private final SharedPreferences preferences;

  @Inject
  public LocalValueStore(SharedPreferences preferences) {
    this.preferences = preferences;
  }

  /** Returns the id of the last project successfully activated by the user, or null if not set. */
  @Nullable
  public String getLastActiveProjectId() {
    return preferences.getString(ACTIVE_PROJECT_ID_KEY, null);
  }

  /** Set the id of the last project successfully activated by the user. */
  public void setLastActiveProjectId(@NonNull String id) {
    preferences.edit().putString(ACTIVE_PROJECT_ID_KEY, id).apply();
  }

  /** Removes all values stored in the local store. */
  public void clear() {
    preferences.edit().clear().apply();
  }

  public boolean shouldUploadMediaOverUnmeteredConnectionOnly() {
    return preferences.getBoolean(Keys.UPLOAD_MEDIA, false);
  }

  public boolean shouldDownloadOfflineAreasOverUnmeteredConnectionOnly() {
    return preferences.getBoolean(Keys.OFFLINE_AREAS, false);
  }

  public void saveMapType(int type) {
    preferences.edit().putInt(MAP_TYPE, type).apply();
  }

  public int getSavedMapType(int defaultType) {
    return preferences.getInt(MAP_TYPE, defaultType);
  }

  public void setLastCameraPosition(String projectId, CameraPosition cameraPosition) {
    Double[] values = {
      cameraPosition.getTarget().getLatitude(),
      cameraPosition.getTarget().getLongitude(),
      (double) cameraPosition.getZoomLevel()
    };
    String value = stream(values).map(String::valueOf).collect(Collectors.joining(","));
    preferences.edit().putString(LAST_VIEWPORT_PREFIX + projectId, value).apply();
  }

  public Optional<CameraPosition> getLastCameraPosition(String projectId) {
    try {
      String value = preferences.getString(LAST_VIEWPORT_PREFIX + projectId, "");
      if (value == null || value.isEmpty()) {
        return Optional.empty();
      }
      String[] values = value.split(",");
      return Optional.of(
          new CameraPosition(
              Point.newBuilder()
                  .setLatitude(Double.parseDouble(values[0]))
                  .setLongitude(Double.parseDouble(values[1]))
                  .build(),
              Float.valueOf(values[2])));
    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
      Timber.e(e, "Invalid camera pos in prefs");
      return Optional.empty();
    }
  }

  /** Returns whether the currently logged in user has accepted the terms or not. */
  public boolean isTermsOfServiceAccepted() {
    return preferences.getBoolean(TOS_ACCEPTED, false);
  }

  /** Updates the terms of service acceptance state for the currently signed in user. */
  public void setTermsOfServiceAccepted(boolean value) {
    preferences.edit().putBoolean(TOS_ACCEPTED, value).apply();
  }

  /** Returns whether the polygon info dialog was previously shown to the user or not. */
  public boolean isPolygonDialogInfoShown() {
    return preferences.getBoolean(POLYGON_INFO_DIALOG, false);
  }

  /** Updates the polygon info dialog value to stop showing the dialog everytime. */
  public void setPolygonInfoDialogShown(boolean value) {
    preferences.edit().putBoolean(POLYGON_INFO_DIALOG, value).apply();
  }
}
