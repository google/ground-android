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

import static com.google.android.gnd.util.Enums.toEnum;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.TypeConverter;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.observation.ResponseDelta;
import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;
import timber.log.Timber;

/**
 * {@link TypeConverter} for converting between {@link ResponseDelta}s and JSON strings used to
 * represent them in the local db.
 */
public class ResponseDeltasTypeConverter {

  private static final String KEY_FIELD_TYPE = "fieldType";
  private static final String KEY_NEW_RESPONSE = "newResponse";

  @TypeConverter
  @NonNull
  public static String toString(@NonNull ImmutableList<ResponseDelta> responseDeltas) {
    JSONObject json = new JSONObject();
    for (ResponseDelta delta : responseDeltas) {
      try {
        JSONObject newJson = new JSONObject();
        newJson
            .put(KEY_FIELD_TYPE, delta.getFieldType().name())
            .put(
                KEY_NEW_RESPONSE,
                delta
                    .getNewResponse()
                    .map(ResponseJsonConverter::toJsonObject)
                    .orElse(JSONObject.NULL));
        json.put(delta.getFieldId(), newJson);
      } catch (JSONException e) {
        Timber.e(e, "Error building JSON");
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
        JSONObject jsonDelta = jsonObject.getJSONObject(fieldId);
        deltas.add(
            ResponseDelta.builder()
                .setFieldId(fieldId)
                .setFieldType(toEnum(Field.Type.class, jsonDelta.getString(KEY_FIELD_TYPE)))
                .setNewResponse(ResponseJsonConverter.toResponse(jsonDelta.get(KEY_NEW_RESPONSE)))
                .build());
      }
    } catch (JSONException e) {
      Timber.e(e, "Error parsing JSON string");
    }
    return deltas.build();
  }
}
