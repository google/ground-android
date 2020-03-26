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

package com.google.android.gnd.persistence.local.room.converter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.TypeConverter;
import com.google.android.gnd.model.observation.ResponseMap;
import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;
import timber.log.Timber;

/**
 * {@link TypeConverter} for converting between {@link ResponseMap} and JSON strings used to
 * represent them in the local db.
 */
public class ResponseMapTypeConverter {

  @TypeConverter
  @Nullable
  public static String toString(@NonNull ResponseMap responseDeltas) {
    JSONObject json = new JSONObject();
    for (String fieldId : responseDeltas.fieldIds()) {
      try {
        json.put(
            fieldId,
            responseDeltas
                .getResponse(fieldId)
                .map(ResponseJsonConverter::toJsonObject)
                .orElse(null));
      } catch (JSONException e) {
        Timber.e(e, "Error building JSON");
      }
    }
    return json.toString();
  }

  @TypeConverter
  @NonNull
  public static ResponseMap fromString(@Nullable String jsonString) {
    ResponseMap.Builder map = ResponseMap.builder();
    if (jsonString == null) {
      return map.build();
    }
    try {
      JSONObject jsonObject = new JSONObject(jsonString);
      Iterator<String> keys = jsonObject.keys();
      while (keys.hasNext()) {
        String fieldId = keys.next();
        ResponseJsonConverter.toResponse(jsonObject.get(fieldId))
            .ifPresent(response -> map.putResponse(fieldId, response));
      }
    } catch (JSONException e) {
      Timber.e(e, "Error parsing JSON string");
    }
    return map.build();
  }
}
