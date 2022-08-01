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

package com.google.android.ground.persistence.remote.firestore.schema;

import static com.google.android.ground.persistence.remote.DataStoreException.checkNotNull;
import static java8.util.stream.StreamSupport.stream;

import com.google.android.ground.model.job.Job;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import com.google.android.ground.model.submission.DateResponse;
import com.google.android.ground.model.submission.MultipleChoiceResponse;
import com.google.android.ground.model.submission.NumberResponse;
import com.google.android.ground.model.submission.ResponseMap;
import com.google.android.ground.model.submission.ResponseMap.Builder;
import com.google.android.ground.model.submission.Submission;
import com.google.android.ground.model.submission.TextResponse;
import com.google.android.ground.model.submission.TimeResponse;
import com.google.android.ground.model.task.MultipleChoice;
import com.google.android.ground.model.task.Task;
import com.google.android.ground.persistence.remote.DataStoreException;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java8.util.Objects;
import javax.annotation.Nullable;
import timber.log.Timber;

/** Converts between Firestore documents and {@link Submission} instances. */
class SubmissionConverter {

  static Submission toSubmission(LocationOfInterest loi, DocumentSnapshot snapshot)
      throws DataStoreException {
    SubmissionDocument doc = snapshot.toObject(SubmissionDocument.class);
    String loiId = checkNotNull(doc.getLoiId(), "loiId");
    if (!loi.getId().equals(loiId)) {
      throw new DataStoreException("Submission doc featureId doesn't match specified loiId");
    }
    // Degrade gracefully when audit info missing in remote db.
    AuditInfoNestedObject created =
        Objects.requireNonNullElse(doc.getCreated(), AuditInfoNestedObject.FALLBACK_VALUE);
    AuditInfoNestedObject lastModified = Objects.requireNonNullElse(doc.getLastModified(), created);
    Job job = loi.getJob();
    return Submission.newBuilder()
        .setId(snapshot.getId())
        .setSurvey(loi.getSurvey())
        .setLocationOfInterest(loi)
        .setJob(job)
        .setResponses(toResponseMap(snapshot.getId(), job, doc.getResponses()))
        .setCreated(AuditInfoConverter.toAuditInfo(created))
        .setLastModified(AuditInfoConverter.toAuditInfo(lastModified))
        .build();
  }

  private static ResponseMap toResponseMap(
      String submissionId, Job job, @Nullable Map<String, Object> docResponses) {
    ResponseMap.Builder responses = ResponseMap.builder();
    if (docResponses == null) {
      return responses.build();
    }
    for (Entry<String, Object> entry : docResponses.entrySet()) {
      String taskId = entry.getKey();
      try {
        putResponse(taskId, job, entry.getValue(), responses);
      } catch (DataStoreException e) {
        Timber.e(e, "Task " + taskId + "in remote db in submission " + submissionId);
      }
    }
    return responses.build();
  }

  private static void putResponse(
      String taskId, Job job, Object obj, ResponseMap.Builder responses) {
    Task task =
        job.getTask(taskId).orElseThrow(() -> new DataStoreException("Not defined in task"));
    switch (task.getType()) {
      case PHOTO:
        // Intentional fall-through.
        // TODO(#755): Handle photo tasks as PhotoResponse instead of TextResponse.
      case TEXT_FIELD:
        putTextResponse(taskId, obj, responses);
        break;
      case MULTIPLE_CHOICE:
        putMultipleChoiceResponse(taskId, task.getMultipleChoice(), obj, responses);
        break;
      case NUMBER:
        putNumberResponse(taskId, obj, responses);
        break;
      case DATE:
        putDateResponse(taskId, obj, responses);
        break;
      case TIME:
        putTimeResponse(taskId, obj, responses);
        break;
      default:
        throw new DataStoreException("Unknown type " + task.getType());
    }
  }

  private static void putNumberResponse(String taskId, Object obj, Builder responses) {
    double value = (Double) DataStoreException.checkType(Double.class, obj);
    NumberResponse.fromNumber(Double.toString(value))
        .ifPresent(r -> responses.putResponse(taskId, r));
  }

  private static void putTextResponse(String taskId, Object obj, ResponseMap.Builder responses) {
    String value = (String) DataStoreException.checkType(String.class, obj);
    TextResponse.fromString(value.trim()).ifPresent(r -> responses.putResponse(taskId, r));
  }

  private static void putDateResponse(String taskId, Object obj, ResponseMap.Builder responses) {
    Timestamp value = (Timestamp) DataStoreException.checkType(Timestamp.class, obj);
    DateResponse.fromDate(value.toDate()).ifPresent(r -> responses.putResponse(taskId, r));
  }

  private static void putTimeResponse(String taskId, Object obj, ResponseMap.Builder responses) {
    Timestamp value = (Timestamp) DataStoreException.checkType(Timestamp.class, obj);
    TimeResponse.fromDate(value.toDate()).ifPresent(r -> responses.putResponse(taskId, r));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void putMultipleChoiceResponse(
      String taskId, MultipleChoice multipleChoice, Object obj, Builder responses) {
    List values = (List) DataStoreException.checkType(List.class, obj);
    stream(values).forEach(v -> DataStoreException.checkType(String.class, v));
    MultipleChoiceResponse.fromList(multipleChoice, (List<String>) values)
        .ifPresent(r -> responses.putResponse(taskId, r));
  }
}
