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

package com.google.android.gnd.service.firestore;

import static com.google.android.gnd.service.firestore.FirestoreDataService.toTimestamps;

import android.util.Log;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.Record;
import com.google.android.gnd.vo.Record.MultipleChoiceValue;
import com.google.android.gnd.vo.Record.TextValue;
import com.google.android.gnd.vo.Record.Value;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java8.util.Optional;

// TODO: Refactor into cleaner peristence layer.
@IgnoreExtraProperties
public class RecordDoc {
  private static final String TAG = RecordDoc.class.getSimpleName();

  public String featureTypeId;

  public String formId;

  public @ServerTimestamp Date serverTimeCreated;

  public @ServerTimestamp Date serverTimeModified;

  public Date clientTimeCreated;

  public Date clientTimeModified;

  public Map<String, Object> responses;

  public static RecordDoc forUpdates(Record record, Map<String, Object> valueUpdates) {
    RecordDoc rd = new RecordDoc();
    rd.featureTypeId = record.getPlace().getPlaceType().getId();
    rd.formId = record.getForm().getId();
    rd.responses = valueUpdates;
    rd.clientTimeModified = new Date();
    return rd;
  }

  public static Record toProto(Place place, String recordId, DocumentSnapshot doc) {
    RecordDoc rd = doc.toObject(RecordDoc.class);
    if (!place.getPlaceType().getId().equals(rd.featureTypeId)) {
      // TODO: Handle error.
    }
    Optional<Form> form = place.getPlaceType().getForm(rd.formId);
    if (!form.isPresent()) {
      // TODO: Handle error.
    }
    return Record.newBuilder()
        .setId(recordId)
        .setProject(place.getProject())
        .setPlace(place)
        .setForm(form.get())
        .putAllValues(convertValues(rd.responses))
        .setServerTimestamps(toTimestamps(rd.serverTimeCreated, rd.serverTimeModified))
        .setClientTimestamps(toTimestamps(rd.clientTimeCreated, rd.clientTimeModified))
        .build();
  }

  private static Map<String, Value> convertValues(Map<String, Object> docValues) {
    Map<String, Value> values = new HashMap<>();
    for (Map.Entry<String, Object> entry : docValues.entrySet()) {
      putValue(values, entry.getKey(), entry.getValue());
    }
    return values;
  }

  private static void putValue(Map<String, Value> values, String fieldId, Object obj) {
    if (obj instanceof String) {
      TextValue.fromString(((String) obj).trim()).ifPresent(v -> values.put(fieldId, v));
      // } else if (obj instanceof Float) {
      //   values.put(key, new NumberValue((Float) obj));
    } else if (obj instanceof List) {
      MultipleChoiceValue.fromList(((List<String>) obj)).ifPresent(v -> values.put(fieldId, v));
    } else {
      Log.d(TAG, "Unsupported obj in db: " + obj.getClass().getName());
    }
  }

  public static Object toObject(Value value) {
    if (value instanceof TextValue) {
      return ((TextValue) value).getText();
    } else if (value instanceof MultipleChoiceValue) {
      return ((MultipleChoiceValue) value).getChoices();
    } else {
      Log.w(TAG, "Unknown value type: " + value.getClass().getName());
      return null;
    }
  }
}
