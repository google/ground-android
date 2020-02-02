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

import android.util.Log;
import androidx.annotation.Nullable;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.observation.MultipleChoiceResponse;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.model.observation.ObservationMutation;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.model.observation.ResponseDelta;
import com.google.android.gnd.model.observation.ResponseMap;
import com.google.android.gnd.model.observation.TextResponse;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.List;
import java.util.Map;
import java8.util.Optional;

// TODO: Refactor into cleaner persistence layer.
@IgnoreExtraProperties
public class ObservationDoc {
  private static final String TAG = ObservationDoc.class.getSimpleName();
  public static final String FEATURE_ID = "featureId";
  public static final String FEATURE_TYPE_ID = "featureTypeId";
  public static final String FORM_ID = "formId";
  public static final String RESPONSES = "responses";
  public static final String CREATED = "created";
  public static final String LAST_MODIFIED = "lastModified";

  @Nullable public String featureId;

  @Nullable public String featureTypeId;

  @Nullable public String formId;

  @Nullable public AuditInfoDoc created;

  @Nullable public AuditInfoDoc modified;

  @Nullable public Map<String, Object> responses;

  public static Observation toObject(Feature feature, String recordId, DocumentSnapshot doc) {
    ObservationDoc rd = doc.toObject(ObservationDoc.class);
    if (!feature.getId().equals(rd.featureId)) {
      // TODO: Handle error.
    }
    if (!feature.getLayer().getId().equals(rd.featureTypeId)) {
      // TODO: Handle error.
    }
    Optional<Form> form = feature.getLayer().getForm(rd.formId);
    if (!form.isPresent()) {
      // TODO: Handle error.
    }
    return Observation.newBuilder()
        .setId(recordId)
        .setProject(feature.getProject())
        .setFeature(feature)
        .setForm(form.get())
        .setResponses(toResponseMap(rd.responses))
        .setCreated(AuditInfoDoc.toObject(rd.created))
        .setLastModified(AuditInfoDoc.toObject(rd.modified))
        .build();
  }

  private static ResponseMap toResponseMap(Map<String, Object> docResponses) {
    ResponseMap.Builder responses = ResponseMap.builder();
    for (String fieldId : docResponses.keySet()) {
      Object obj = docResponses.get(fieldId);
      if (obj instanceof String) {
        TextResponse.fromString(fieldId, ((String) obj).trim())
            .ifPresent(r -> responses.putResponse(fieldId, r));
        // TODO(#23): Implement number fields, e.g.:
        // } else if (obj instanceof Float) {
        //   responses.put(key, new NumericResponse((Float) obj));
      } else if (obj instanceof List) {
        MultipleChoiceResponse.fromList(fieldId, (List<String>) obj)
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

  public static ImmutableMap<String, Object> toMap(ObservationMutation mutation, User user) {
    ImmutableMap.Builder<String, Object> map = ImmutableMap.builder();
    AuditInfoDoc auditInfo = AuditInfoDoc.fromMutationAndUser(mutation, user);
    switch (mutation.getType()) {
      case CREATE:
        map.put(CREATED, auditInfo);
        map.put(LAST_MODIFIED, auditInfo);
        break;
      case UPDATE:
        map.put(LAST_MODIFIED, auditInfo);
        break;
      case DELETE:
        // TODO.
      case UNKNOWN:
        throw new UnsupportedOperationException();
      default:
        Log.e(TAG, "Unhandled mutation type: " + mutation.getType());
        break;
    }
    map.put(FEATURE_ID, mutation.getFeatureId())
        .put(FEATURE_TYPE_ID, mutation.getLayerId())
        .put(FORM_ID, mutation.getFormId())
        .put(RESPONSES, toMap(mutation.getResponseDeltas()));
    return map.build();
  }

  private static Map<String, Object> toMap(ImmutableList<ResponseDelta> responseDeltas) {
    ImmutableMap.Builder<String, Object> map = ImmutableMap.builder();
    for (ResponseDelta delta : responseDeltas) {
      map.put(
          delta.getFieldId(),
          delta.getNewResponse().map(ObservationDoc::toObject).orElse(FieldValue.delete()));
    }
    return map.build();
  }
}
