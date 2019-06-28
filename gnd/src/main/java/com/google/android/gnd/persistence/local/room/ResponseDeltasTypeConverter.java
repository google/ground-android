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

package com.google.android.gnd.persistence.local.room;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.TypeConverter;
import com.google.android.gnd.persistence.shared.ResponseDelta;
import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link TypeConverter} for converting between {@link ResponseDelta}s and JSON strings used to
 * represent them in the local db.
 */
public class ResponseDeltasTypeConverter {
  private static final String TAG = ResponseDeltasTypeConverter.class.getSimpleName();

  @TypeConverter
  @Nullable
  public static String toString(@NonNull ImmutableList<ResponseDelta> responseDeltas) {
    JSONObject json = new JSONObject();
    for (ResponseDelta delta : responseDeltas) {
      try {
        json.put(
            delta.getFieldId(),
            delta.getNewResponse().map(ResponseJsonConverter::toJsonObject).orElse(null));
      } catch (JSONException e) {
        Log.e(TAG, "Error building JSON", e);
      }
    }
    return json.toString();
  }

  @TypeConverter
  @NonNull
  public static ImmutableList<ResponseDelta> fromString(@Nullable String jsonString) {
    ImmutableList.Builder<ResponseDelta> deltas = ImmutableList.builder();
    if (jsonString == null) {
      return deltas.build();
    }
    try {
      JSONObject jsonObject = new JSONObject(jsonString);
      Iterator<String> keys = jsonObject.keys();
      while (keys.hasNext()) {
        String fieldId = keys.next();
        deltas.add(
            ResponseDelta.builder()
                .setFieldId(fieldId)
                .setNewResponse(ResponseJsonConverter.toResponse(jsonObject.get(fieldId)))
                .build());
      }
    } catch (JSONException e) {
      Log.e(TAG, "Error parsing JSON string", e);
    }
    return deltas.build();
  }
}
