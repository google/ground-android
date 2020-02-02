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

import static java8.lang.Iterables.forEach;

import android.util.Log;
import com.google.android.gnd.model.observation.MultipleChoiceResponse;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.model.observation.TextResponse;
import java.util.ArrayList;
import java.util.List;
import java8.util.Optional;
import org.json.JSONArray;
import org.json.JSONException;

class ResponseJsonConverter {
  private static final String TAG = ResponseJsonConverter.class.getSimpleName();

  public static Object toJsonObject(Response response) {
    if (response instanceof TextResponse) {
      return ((TextResponse) response).getText();
    } else if (response instanceof MultipleChoiceResponse) {
      return toJsonArray((MultipleChoiceResponse) response);
    } else {
      throw new UnsupportedOperationException("Unimplemented Response " + response.getClass());
    }
  }

  private static Object toJsonArray(MultipleChoiceResponse response) {
    JSONArray array = new JSONArray();
    forEach(response.getChoices(), array::put);
    return array;
  }

  public static Optional<Response> toResponse(String fieldId, Object obj) {
    if (obj instanceof String) {
      return TextResponse.fromString(fieldId, (String) obj);
    } else if (obj instanceof JSONArray) {
      return MultipleChoiceResponse.fromList(toList((JSONArray) obj));
    } else {
      Log.e(TAG, "Error parsing JSON in db of " + obj.getClass() + ": " + obj);
      return Optional.empty();
    }
  }

  private static List<String> toList(JSONArray jsonArray) {
    List<String> list = new ArrayList<>(jsonArray.length());
    for (int i = 0; i < jsonArray.length(); i++) {
      try {
        list.add(jsonArray.getString(i));
      } catch (JSONException e) {
        Log.e(TAG, "Error parsing JSONArray in db: " + jsonArray);
      }
    }
    return list;
  }
}
