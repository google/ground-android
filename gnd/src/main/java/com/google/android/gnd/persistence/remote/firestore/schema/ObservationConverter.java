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
import java8.util.Objects;
import javax.annotation.Nullable;
import timber.log.Timber;

/** Converts between Firestore documents and {@link Observation} instances. */
class ObservationConverter {

  static Observation toObservation(Feature feature, DocumentSnapshot snapshot)
      throws DataStoreException {
    ObservationDocument doc = snapshot.toObject(ObservationDocument.class);
    String featureId = checkNotNull(doc.getFeatureId(), "featureId");
    String formId = checkNotNull(doc.getFormId(), "formId");
    if (!feature.getId().equals(featureId)) {
      Timber.e("Observation featureId doesn't match specified feature id");
    }
    Form form = checkNotEmpty(feature.getLayer().getForm(formId), "form");
    // Degrade gracefully when audit info missing in remote db.
    AuditInfoNestedObject created =
        Objects.requireNonNullElse(doc.getCreated(), AuditInfoNestedObject.FALLBACK_VALUE);
    AuditInfoNestedObject lastModified = Objects.requireNonNullElse(doc.getLastModified(), created);
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
        // TODO(#23): Add support for number fields:
        // } else if (obj instanceof Float) {
        //   responses.put(key, new NumericResponse((Float) obj));
      } else if (obj instanceof List) {
        MultipleChoiceResponse.fromList((List<String>) obj)
            .ifPresent(r -> responses.putResponse(fieldId, r));
      } else {
        Timber.e("Unsupported obj in db: %s", obj.getClass().getName());
      }
    }
    return responses.build();
  }
}
