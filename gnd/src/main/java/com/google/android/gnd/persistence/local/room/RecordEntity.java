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
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.android.gnd.persistence.shared.RecordMutation;
import com.google.android.gnd.vo.Record.MultipleChoiceResponse;
import com.google.android.gnd.vo.Record.Response;
import com.google.android.gnd.vo.Record.TextResponse;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import java.util.Map;
import java.util.Map.Entry;
import java8.util.Optional;
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
      return ((MultipleChoiceResponse) response).getChoices();
    } else {
      throw new UnsupportedOperationException("Unimplemented Response " + response.getClass());
    }
  }

  // Auto-generated boilerplate:

  public static RecordEntity create(
      String id, EntityState state, String formId, JSONObject responses) {
    return builder().setId(id).setState(state).setResponses(responses).setFormId(formId).build();
  }

  public static Builder builder() {
    return new AutoValue_RecordEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String newId);

    public abstract Builder setState(EntityState newState);

    public abstract Builder setFormId(String newFormId);

    public abstract Builder setResponses(JSONObject newResponses);

    public abstract RecordEntity build();
  }
}
