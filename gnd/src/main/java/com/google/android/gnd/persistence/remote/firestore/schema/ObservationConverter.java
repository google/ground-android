/*
 * Copyright 2020 Google LLC
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

package com.google.android.gnd.persistence.remote.firestore.schema;

import static com.google.android.gnd.persistence.remote.DataStoreException.checkNotEmpty;
import static com.google.android.gnd.persistence.remote.DataStoreException.checkNotNull;

import android.util.Log;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.observation.MultipleChoiceResponse;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.model.observation.ResponseMap;
import com.google.android.gnd.model.observation.TextResponse;
import com.google.android.gnd.persistence.remote.DataStoreException;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.List;
import java.util.Map;
import java8.util.Optional;
import javax.annotation.Nullable;

/** Converts between Firestore documents and {@link Observation} instances. */
class ObservationConverter {

  private static final String TAG = ObservationConverter.class.getSimpleName();

  static Observation toObservation(Feature feature, DocumentSnapshot snapshot)
      throws DataStoreException {
    ObservationDocument doc = snapshot.toObject(ObservationDocument.class);
    String featureId = checkNotNull(doc.getFeatureId(), "featureId");
    String layerId = checkNotNull(doc.getlayerId(), "layerId");
    String formId = checkNotNull(doc.getFormId(), "formId");
    if (!feature.getId().equals(featureId)) {
      Log.w(TAG, "Observation featureId doesn't match specified feature id");
    }
    if (!feature.getLayer().getId().equals(layerId)) {
      Log.w(TAG, "Observation layerId doesn't match specified feature's layerId");
    }
    Form form = checkNotEmpty(feature.getLayer().getForm(formId), "form");
    AuditInfoNestedObject created = checkNotNull(doc.getCreated(), "created");
    AuditInfoNestedObject lastModified = Optional.ofNullable(doc.getLastModified()).orElse(created);
    return Observation.newBuilder()
        .setId(snapshot.getId())
        .setProject(feature.getProject())
        .setFeature(feature)
        .setForm(form)
        .setResponses(toResponseMap(doc.getResponses()))
        .setCreated(AuditInfoConverter.toAuditInfo(created))
        .setLastModified(AuditInfoConverter.toAuditInfo(lastModified))
        .build();
  }

  private static ResponseMap toResponseMap(@Nullable Map<String, Object> docResponses) {
    ResponseMap.Builder responses = ResponseMap.builder();
    if (docResponses == null) {
      return responses.build();
    }
    for (String fieldId : docResponses.keySet()) {
      Object obj = docResponses.get(fieldId);
      if (obj instanceof String) {
        TextResponse.fromString(((String) obj).trim())
            .ifPresent(r -> responses.putResponse(fieldId, r));
        // TODO(#23): Implement number fields, e.g.:
        // } else if (obj instanceof Float) {
        //   responses.put(key, new NumericResponse((Float) obj));
      } else if (obj instanceof List) {
        MultipleChoiceResponse.fromList((List<String>) obj)
            .ifPresent(r -> responses.putResponse(fieldId, r));
      } else {
        Log.d(TAG, "Unsupported obj in db: " + obj.getClass().getName());
      }
    }
    return responses.build();
  }
}
