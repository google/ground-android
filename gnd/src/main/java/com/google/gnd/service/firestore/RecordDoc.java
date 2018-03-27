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

package com.google.gnd.service.firestore;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.gnd.model.Record;
import com.google.gnd.model.Record.Value;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.gnd.service.firestore.FirestoreDataService.toDate;
import static com.google.gnd.service.firestore.FirestoreDataService.toTimestamps;

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

  public static RecordDoc fromProto(Record r, Map<String, Object> valueUpdates) {
    RecordDoc rd = new RecordDoc();
    rd.featureTypeId = r.getFeatureTypeId();
    rd.formId = r.getFormId();
    rd.responses = valueUpdates;
    if (r.getServerTimestamps().hasCreated()) {
      rd.serverTimeCreated = toDate(r.getServerTimestamps().getCreated());
    }
    if (r.getServerTimestamps().hasModified()) {
      rd.serverTimeModified = toDate(r.getServerTimestamps().getModified());
    }
    if (r.getClientTimestamps().hasCreated()) {
      rd.clientTimeCreated = toDate(r.getClientTimestamps().getCreated());
    }
    if (r.getClientTimestamps().hasModified()) {
      rd.clientTimeModified = toDate(r.getClientTimestamps().getModified());
    }
    return rd;
  }

  public static Record toProto(String id, DocumentSnapshot doc) {
    RecordDoc rd = doc.toObject(RecordDoc.class);
    return Record.newBuilder()
        .setId(id)
        .setFeatureTypeId(rd.featureTypeId)
        .setFormId(rd.formId)
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

  private static void putValue(Map<String, Value> values, String key, Object value) {
    Value.Builder builder = Value.newBuilder();
    if (value instanceof String) {
      builder.setText((String) value);
    } else if (value instanceof Float) {
      builder.setNumber((Float) value);
    } else if (value instanceof List) {
      builder.setChoices(Record.Choices.newBuilder().addAllCodes((List<String>) value));
    } else {
      Log.d(TAG, "Unsupported value in db: " + value.getClass().getName());
      return;
    }
    values.put(key, builder.build());
  }

  public static Object toObject(Value value) {
    switch (value.getTypeCase()) {
      case TEXT:
        return value.getText();
      case NUMBER:
        return value.getNumber();
      case CHOICES:
        return value.getChoices().getCodesList();
      default:
        Log.d(TAG, "Unsupported value in client: " + value.getClass().getName());
        return "";
    }
  }
}
