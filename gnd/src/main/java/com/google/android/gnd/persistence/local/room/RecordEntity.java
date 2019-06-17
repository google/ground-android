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
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.android.gnd.persistence.shared.RecordMutation;
import com.google.android.gnd.vo.Feature;
import com.google.android.gnd.vo.Record;
import com.google.android.gnd.vo.Record.MultipleChoiceResponse;
import com.google.android.gnd.vo.Record.Response;
import com.google.android.gnd.vo.Record.TextResponse;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java8.util.Optional;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Representation of a {@link com.google.android.gnd.vo.Record} in local db. */
@AutoValue
@Entity(
    tableName = "record",
    indices = {@Index("id")})
public abstract class RecordEntity {

  private static final String TAG = RecordEntity.class.getSimpleName();

  @CopyAnnotations
  @PrimaryKey
  @ColumnInfo(name = "id")
  @NonNull
  public abstract String getId();

  /** Returns the id of the feature to which this record applies. */
  @CopyAnnotations
  @ColumnInfo(name = "feature_id")
  @NonNull
  public abstract String getFeatureId();

  /** Returns the id of the form to which this record's responses apply. */
  @CopyAnnotations
  @ColumnInfo(name = "form_id")
  @NonNull
  public abstract String getFormId();

  @CopyAnnotations
  @ColumnInfo(name = "state")
  @NonNull
  public abstract EntityState getState();

  /**
   * Returns a JSON object containing user responses keyed by their respective elementId in the form
   * identified by formId. Returns an empty JSON object if no responses have been provided.
   */
  @CopyAnnotations
  @ColumnInfo(name = "responses")
  @NonNull
  public abstract JSONObject getResponses();

  public static RecordEntity fromMutation(RecordMutation mutation) {
    return RecordEntity.builder()
        .setId(mutation.getRecordId())
        .setFormId(mutation.getFormId())
        .setFeatureId(mutation.getFeatureId())
        .setState(EntityState.DEFAULT)
        .setResponses(convertResponsesToJson(mutation.getModifiedResponses()))
        .build();
  }

  static JSONObject convertResponsesToJson(Map<String, Optional<Response>> responses) {
    JSONObject json = new JSONObject();
    for (Entry<String, Optional<Response>> entry : responses.entrySet()) {
      String elementId = entry.getKey();
      Optional<Response> response = entry.getValue();
      try {
        json.put(elementId, response.map(RecordEntity::toJsonObject).orElse(null));
      } catch (JSONException e) {
        Log.e(TAG, "Error building JSON", e);
      }
    }
    return json;
  }

  private static Object toJsonObject(Response response) {
    if (response instanceof TextResponse) {
      return ((TextResponse) response).getText();
    } else if (response instanceof MultipleChoiceResponse) {
      return toJSONArray((MultipleChoiceResponse) response);
    } else {
      throw new UnsupportedOperationException("Unimplemented Response " + response.getClass());
    }
  }

  private static Object toJSONArray(MultipleChoiceResponse response) {
    JSONArray array = new JSONArray();
    forEach(response.getChoices(), array::put);
    return array;
  }

  // TODO(#127): Replace reference to Feature in Record with featureId and remove feature arg.
  public static Record toRecord(Feature feature, RecordEntity record) {
    // TODO(#127): Replace reference to Form in Record with formId and remove here.
    // TODO(#127): Replace reference to Project in Record with projectId and remove here.
    return Record.newBuilder()
        .setId(record.getId())
        .setForm(feature.getFeatureType().getForm(record.getFormId()).get())
        .setProject(feature.getProject())
        .setFeature(feature)
        .putAllResponses(toResponseMap(record.getResponses()))
        .build();
  }

  private static Map<String, Response> toResponseMap(JSONObject responses) {
    Map<String, Response> responseMap = new HashMap<>();
    Iterator<String> keys = responses.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      try {
        fromJsonObject(responses.get(key)).ifPresent(value -> responseMap.put(key, value));
      } catch (JSONException e) {
        Log.e(TAG, "Error getting response value", e);
      }
    }
    return responseMap;
  }

  private static Optional<Response> fromJsonObject(Object obj) {
    if (obj instanceof String) {
      return TextResponse.fromString((String) obj);
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

  // Auto-generated boilerplate:

  public static RecordEntity create(
      String id, EntityState state, String featureId, String formId, JSONObject responses) {
    return builder()
        .setId(id)
        .setState(state)
        .setFeatureId(featureId)
        .setResponses(responses)
        .setFormId(formId)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_RecordEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String newId);

    public abstract Builder setState(EntityState newState);

    public abstract Builder setFeatureId(String newFeatureId);

    public abstract Builder setFormId(String newFormId);

    public abstract Builder setResponses(JSONObject newResponses);

    public abstract RecordEntity build();
  }
}
