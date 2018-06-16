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
import com.google.android.gnd.vo.Record;
import com.google.android.gnd.vo.Record.Value;
import com.google.common.collect.ImmutableList;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    rd.featureTypeId = r.getPlaceTypeId();
    rd.formId = r.getFormId();
    rd.responses = valueUpdates;
    rd.serverTimeCreated = r.getServerTimestamps().getCreated();
    rd.serverTimeModified = r.getServerTimestamps().getModified();
    rd.clientTimeCreated = r.getClientTimestamps().getCreated();
    rd.clientTimeModified = r.getClientTimestamps().getModified();
    return rd;
  }

  public static Record toProto(String id, DocumentSnapshot doc) {
    RecordDoc rd = doc.toObject(RecordDoc.class);
    return Record.newBuilder()
        .setId(id)
        .setPlaceTypeId(rd.featureTypeId)
        .setFormId(rd.formId)
        .setValueMap(convertValues(rd.responses))
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
    if (value instanceof String) {
      values.put(key, Value.ofText((String) value));
    } else if (value instanceof Float) {
      values.put(key, Value.ofNumber((Float) value));
    } else if (value instanceof List) {
      values.put(
          key,
          Value.ofChoices(
              Record.Choices.newBuilder().setCodes(ImmutableList.copyOf((List) value)).build()));
    } else {
      Log.d(TAG, "Unsupported value in db: " + value.getClass().getName());
    }
  }

  public static Object toObject(Value value) {
    switch (value.getType()) {
      case TEXT:
        return value.getText();
      case NUMBER:
        return value.getNumber();
      case CHOICES:
        return value.getChoices().getCodes();
      default:
        Log.d(TAG, "Unsupported value in client: " + value.getClass().getName());
        return "";
    }
  }
}
