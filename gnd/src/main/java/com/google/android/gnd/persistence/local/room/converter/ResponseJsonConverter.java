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

import static java8.lang.Iterables.forEach;

import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.observation.MultipleChoiceResponse;
import com.google.android.gnd.model.observation.NumberResponse;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.model.observation.TextResponse;
import com.google.android.gnd.persistence.remote.DataStoreException;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java8.util.Optional;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import timber.log.Timber;

class ResponseJsonConverter {

  static Object toJsonObject(Response response) {
    if (response instanceof TextResponse) {
      return ((TextResponse) response).getText();
    } else if (response instanceof MultipleChoiceResponse) {
      return toJsonArray((MultipleChoiceResponse) response);
    } else if (response instanceof NumberResponse) {
      double value = ((NumberResponse) response).getValue();
      if (Double.isNaN(value)) {
        return JSONObject.NULL;
      }
      return value;
    } else {
      throw new UnsupportedOperationException("Unimplemented Response " + response.getClass());
    }
  }

  private static Object toJsonArray(MultipleChoiceResponse response) {
    JSONArray array = new JSONArray();
    forEach(response.getChoices(), array::put);
    return array;
  }

  static Optional<Response> toResponse(Field field, Object obj) {
    switch (field.getType()) {
      case TEXT_FIELD:
      case PHOTO:
        DataStoreException.checkType(String.class, obj);
        return TextResponse.fromString((String) obj);
      case MULTIPLE_CHOICE:
        DataStoreException.checkType(JSONArray.class, obj);
        return MultipleChoiceResponse.fromList(toList((JSONArray) obj));
      case NUMBER:
        if (JSONObject.NULL == obj) {
          return NumberResponse.fromNumber(Double.NaN);
        }
        DataStoreException.checkType(Number.class, obj);
        return NumberResponse.fromNumber((Number) obj);
      case UNKNOWN:
      default:
        throw new DataStoreException("Unknown type in field: " + obj.getClass().getName());
    }
  }

  private static ImmutableList<String> toList(JSONArray jsonArray) {
    List<String> list = new ArrayList<>(jsonArray.length());
    for (int i = 0; i < jsonArray.length(); i++) {
      try {
        list.add(jsonArray.getString(i));
      } catch (JSONException e) {
        Timber.e("Error parsing JSONArray in db: %s", jsonArray);
      }
    }
    return ImmutableList.<String>builder().addAll(list).build();
  }
}
