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

import androidx.annotation.Nullable;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.Map;

/**
 * Submission entity stored in Firestore.
 */
@IgnoreExtraProperties
class SubmissionDocument {

  @Nullable private String loiId;
  @Nullable private String taskId;
  @Nullable private AuditInfoNestedObject created;
  @Nullable private AuditInfoNestedObject lastModified;
  @Nullable private Map<String, Object> responses;

  @SuppressWarnings("unused")
  public SubmissionDocument() {
  }

  @SuppressWarnings("unused")
  SubmissionDocument(
      @Nullable String loiId,
      @Nullable String taskId,
      @Nullable AuditInfoNestedObject created,
      @Nullable AuditInfoNestedObject lastModified,
      @Nullable Map<String, Object> responses) {
    this.loiId = loiId;
    this.taskId = taskId;
    this.created = created;
    this.lastModified = lastModified;
    this.responses = responses;
  }

  @Nullable
  public String getLoiId() {
    return loiId;
  }

  @Nullable
  public String getTaskId() {
    return taskId;
  }

  @Nullable
  public AuditInfoNestedObject getCreated() {
    return created;
  }

  @Nullable
  public AuditInfoNestedObject getLastModified() {
    return lastModified;
  }

  @Nullable
  public Map<String, Object> getResponses() {
    return responses;
  }
}
