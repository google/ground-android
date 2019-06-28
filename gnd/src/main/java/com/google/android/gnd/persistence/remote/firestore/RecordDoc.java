/*
 * Copyright 2018 Google LLC
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

package com.google.android.gnd.persistence.remote.firestore;

import static com.google.android.gnd.persistence.remote.firestore.FirestoreDataStore.toTimestamps;

import android.util.Log;
import androidx.annotation.Nullable;
import com.google.android.gnd.persistence.shared.RecordMutation;
import com.google.android.gnd.persistence.shared.ResponseDelta;
import com.google.android.gnd.vo.Feature;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Record;
import com.google.android.gnd.vo.Record.MultipleChoiceResponse;
import com.google.android.gnd.vo.Record.Response;
import com.google.android.gnd.vo.Record.TextResponse;
import com.google.android.gnd.vo.ResponseMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java8.util.Optional;

// TODO: Refactor into cleaner persistence layer.
@IgnoreExtraProperties
public class RecordDoc {
  private static final String TAG = RecordDoc.class.getSimpleName();
  private static final String FEATURE_ID = "featureId";
  private static final String FEATURE_TYPE_ID = "featureTypeId";
  private static final String FORM_ID = "formId";
  private static final String RESPONSES = "responses";

  @Nullable public String featureId;

  @Nullable public String featureTypeId;

  @Nullable public String formId;

  @Nullable public UserDoc createdBy;

  @Nullable public UserDoc modifiedBy;

  public @Nullable @ServerTimestamp Date serverTimeCreated;

  public @Nullable @ServerTimestamp Date serverTimeModified;

  @Nullable public Date clientTimeCreated;

  @Nullable public Date clientTimeModified;

  @Nullable public Map<String, Object> responses;

  public static RecordDoc forUpdates(Record record, Map<String, Object> responseUpdates) {
    RecordDoc rd = new RecordDoc();
    rd.featureId = record.getFeature().getId();
    rd.featureTypeId = record.getFeature().getFeatureType().getId();
    rd.formId = record.getForm().getId();
    rd.responses = responseUpdates;
    rd.clientTimeModified = new Date();
    rd.createdBy = UserDoc.fromObject(record.getCreatedBy());
    rd.modifiedBy = UserDoc.fromObject(record.getModifiedBy());
    return rd;
  }

  public static Record toObject(Feature feature, String recordId, DocumentSnapshot doc) {
    RecordDoc rd = doc.toObject(RecordDoc.class);
    if (!feature.getId().equals(rd.featureId)) {
      // TODO: Handle error.
    }
    if (!feature.getFeatureType().getId().equals(rd.featureTypeId)) {
      // TODO: Handle error.
    }
    Optional<Form> form = feature.getFeatureType().getForm(rd.formId);
    if (!form.isPresent()) {
      // TODO: Handle error.
    }
    return Record.newBuilder()
        .setId(recordId)
        .setProject(feature.getProject())
        .setFeature(feature)
        .setForm(form.get())
        .setResponses(toResponseMap(rd.responses))
        .setCreatedBy(UserDoc.toObject(rd.createdBy))
        .setModifiedBy(UserDoc.toObject(rd.modifiedBy))
        .setServerTimestamps(toTimestamps(rd.serverTimeCreated, rd.serverTimeModified))
        .setClientTimestamps(toTimestamps(rd.clientTimeCreated, rd.clientTimeModified))
        .build();
  }

  private static ResponseMap toResponseMap(Map<String, Object> docResponses) {
    ResponseMap.Builder responses = ResponseMap.builder();
    for (String fieldId : docResponses.keySet()) {
      Object obj = docResponses.get(fieldId);
      if (obj instanceof String) {
        TextResponse.fromString(((String) obj).trim())
            .ifPresent(r -> responses.putResponse(fieldId, r));
        // TODO(#23): Implement number fields, e.g.:
        // } else if (obj instanceof Float) {
        //   responses.put(key, new NumericResponse((Float) obj));
      } else if (obj instanceof List) {
        MultipleChoiceResponse.fromList(((List<String>) obj))
            .ifPresent(r -> responses.putResponse(fieldId, r));
      } else {
        Log.d(TAG, "Unsupported obj in db: " + obj.getClass().getName());
      }
    }
    return responses.build();
  }

  public static Object toObject(Response response) {
    if (response instanceof TextResponse) {
      return ((TextResponse) response).getText();
    } else if (response instanceof MultipleChoiceResponse) {
      return ((MultipleChoiceResponse) response).getChoices();
    } else {
      Log.w(TAG, "Unknown response type: " + response.getClass().getName());
      return null;
    }
  }

  public static ImmutableMap<String, Object> toMap(RecordMutation mutation) {
    return ImmutableMap.<String, Object>builder()
        .put(FEATURE_ID, mutation.getFeatureId())
        // TODO: Add featureTypeId.
        .put(FORM_ID, mutation.getFormId())
        .put(RESPONSES, toMap(mutation.getResponseDeltas()))
        // TODO: Set user id and timestamps.
        // TODO: Don't echo server timestamp in client. When we implement a proper DAL we can
        .build();
  }

  private static Map<String, Object> toMap(ImmutableList<ResponseDelta> responseDeltas) {
    ImmutableMap.Builder<String, Object> map = ImmutableMap.builder();
    for (ResponseDelta delta : responseDeltas) {
      map.put(
          delta.getFieldId(),
          delta.getNewResponse().map(RecordDoc::toObject).orElse(FieldValue.delete()));
    }
    return map.build();
  }
}
