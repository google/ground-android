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
import static java8.util.stream.StreamSupport.stream;

import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.form.MultipleChoice;
import com.google.android.gnd.model.observation.DateResponse;
import com.google.android.gnd.model.observation.MultipleChoiceResponse;
import com.google.android.gnd.model.observation.NumberResponse;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.model.observation.ResponseMap;
import com.google.android.gnd.model.observation.ResponseMap.Builder;
import com.google.android.gnd.model.observation.TextResponse;
import com.google.android.gnd.model.observation.TimeResponse;
import com.google.android.gnd.persistence.remote.DataStoreException;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java8.util.Objects;
import javax.annotation.Nullable;
import timber.log.Timber;

/** Converts between Firestore documents and {@link Observation} instances. */
class ObservationConverter {

  static Observation toObservation(Feature feature, DocumentSnapshot snapshot)
      throws DataStoreException {
    ObservationDocument doc = snapshot.toObject(ObservationDocument.class);
    String featureId = checkNotNull(doc.getFeatureId(), "featureId");
    if (!feature.getId().equals(featureId)) {
      throw new DataStoreException("Observation doc featureId doesn't match specified feature id");
    }
    String formId = checkNotNull(doc.getFormId(), "formId");
    Form form = checkNotEmpty(feature.getLayer().getForm(formId), "form " + formId);
    // Degrade gracefully when audit info missing in remote db.
    AuditInfoNestedObject created =
        Objects.requireNonNullElse(doc.getCreated(), AuditInfoNestedObject.FALLBACK_VALUE);
    AuditInfoNestedObject lastModified = Objects.requireNonNullElse(doc.getLastModified(), created);
    return Observation.newBuilder()
        .setId(snapshot.getId())
        .setProject(feature.getProject())
        .setFeature(feature)
        .setForm(form)
        .setResponses(toResponseMap(snapshot.getId(), form, doc.getResponses()))
        .setCreated(AuditInfoConverter.toAuditInfo(created))
        .setLastModified(AuditInfoConverter.toAuditInfo(lastModified))
        .build();
  }

  private static ResponseMap toResponseMap(
      String observationId, Form form, @Nullable Map<String, Object> docResponses) {
    ResponseMap.Builder responses = ResponseMap.builder();
    if (docResponses == null) {
      return responses.build();
    }
    for (Entry<String, Object> entry : docResponses.entrySet()) {
      String fieldId = entry.getKey();
      try {
        putResponse(fieldId, form, entry.getValue(), responses);
      } catch (DataStoreException e) {
        Timber.e(e, "Field " + fieldId + "in remote db in observation " + observationId);
      }
    }
    return responses.build();
  }

  private static void putResponse(
      String fieldId, Form form, Object obj, ResponseMap.Builder responses) {
    Field field =
        form.getField(fieldId).orElseThrow(() -> new DataStoreException("Not defined in form"));
    switch (field.getType()) {
      case PHOTO:
        // Intentional fall-through.
        // TODO(#755): Handle photo fields as PhotoResponse instead of TextResponse.
      case TEXT_FIELD:
        putTextResponse(fieldId, obj, responses);
        break;
      case MULTIPLE_CHOICE:
        putMultipleChoiceResponse(fieldId, field.getMultipleChoice(), obj, responses);
        break;
      case NUMBER:
        putNumberResponse(fieldId, obj, responses);
        break;
      case DATE:
        putDateResponse(fieldId, obj, responses);
        break;
      case TIME:
        putTimeResponse(fieldId, obj, responses);
        break;
      default:
        throw new DataStoreException("Unknown type " + field.getType());
    }
  }

  private static void putNumberResponse(String fieldId, Object obj, Builder responses) {
    double value = (Double) DataStoreException.checkType(Double.class, obj);
    NumberResponse.fromNumber(Double.toString(value))
        .ifPresent(r -> responses.putResponse(fieldId, r));
  }

  private static void putTextResponse(String fieldId, Object obj, ResponseMap.Builder responses) {
    String value = (String) DataStoreException.checkType(String.class, obj);
    TextResponse.fromString(value.trim()).ifPresent(r -> responses.putResponse(fieldId, r));
  }

  private static void putDateResponse(String fieldId, Object obj, ResponseMap.Builder responses) {
    Timestamp value = (Timestamp) DataStoreException.checkType(Timestamp.class, obj);
    DateResponse.fromDate(value.toDate()).ifPresent(r -> responses.putResponse(fieldId, r));
  }

  private static void putTimeResponse(String fieldId, Object obj, ResponseMap.Builder responses) {
    Timestamp value = (Timestamp) DataStoreException.checkType(Timestamp.class, obj);
    TimeResponse.fromDate(value.toDate()).ifPresent(r -> responses.putResponse(fieldId, r));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void putMultipleChoiceResponse(
      String fieldId, MultipleChoice multipleChoice, Object obj, Builder responses) {
    List values = (List) DataStoreException.checkType(List.class, obj);
    stream(values).forEach(v -> DataStoreException.checkType(String.class, v));
    MultipleChoiceResponse.fromList(multipleChoice, (List<String>) values)
        .ifPresent(r -> responses.putResponse(fieldId, r));
  }
}
