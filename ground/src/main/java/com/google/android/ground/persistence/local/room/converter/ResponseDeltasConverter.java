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

package com.google.android.ground.persistence.local.room.converter;

import static com.google.android.ground.util.Enums.toEnum;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.ground.model.job.Job;
import com.google.android.ground.model.submission.ResponseDelta;
import com.google.android.ground.model.task.Task;
import com.google.android.ground.model.task.Task;
import com.google.android.ground.persistence.local.LocalDataConsistencyException;
import com.google.android.ground.persistence.remote.DataStoreException;
import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;
import timber.log.Timber;

/**
 * Converts between {@link ResponseDelta}s and JSON strings used to represent them in the local db.
 */
public class ResponseDeltasConverter {

  private static final String KEY_TASK_TYPE = "taskType";
  private static final String KEY_NEW_RESPONSE = "newResponse";

  @NonNull
  public static String toString(@NonNull ImmutableList<ResponseDelta> responseDeltas) {
    JSONObject json = new JSONObject();
    for (ResponseDelta delta : responseDeltas) {
      try {
        JSONObject newJson = new JSONObject();
        newJson
            .put(KEY_TASK_TYPE, delta.getTaskType().name())
            .put(
                KEY_NEW_RESPONSE,
                delta
                    .getNewResponse()
                    .map(ResponseJsonConverter::toJsonObject)
                    .orElse(JSONObject.NULL));
        json.put(delta.getTaskId(), newJson);
      } catch (JSONException e) {
        Timber.e(e, "Error building JSON");
      }
    }
    return json.toString();
  }

  @NonNull
  public static ImmutableList<ResponseDelta> fromString(Job job, @Nullable String jsonString) {
    ImmutableList.Builder<ResponseDelta> deltas = ImmutableList.builder();
    if (jsonString == null) {
      return deltas.build();
    }
    try {
      JSONObject jsonObject = new JSONObject(jsonString);
      Iterator<String> keys = jsonObject.keys();
      while (keys.hasNext()) {
        try {
          String taskId = keys.next();
          Task task =
              job.getTask(taskId)
                  .orElseThrow(
                      () -> new LocalDataConsistencyException("Unknown task id " + taskId));
          JSONObject jsonDelta = jsonObject.getJSONObject(taskId);
          deltas.add(
              ResponseDelta.builder()
                  .setTaskId(taskId)
                  .setTaskType(toEnum(Task.Type.class, jsonDelta.getString(KEY_TASK_TYPE)))
                  .setNewResponse(
                      ResponseJsonConverter.toResponse(task, jsonDelta.get(KEY_NEW_RESPONSE)))
                  .build());
        } catch (LocalDataConsistencyException | DataStoreException e) {
          Timber.d("Bad response in local db: " + e.getMessage());
        }
      }
    } catch (JSONException e) {
      Timber.e(e, "Error parsing JSON string");
    }
    return deltas.build();
  }
}
