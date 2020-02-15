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
import com.google.android.gnd.persistence.remote.firestore.AuditInfoDoc;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.Map;

/** Observation entity stored in Firestore. */
@IgnoreExtraProperties
public class ObservationDocument {
  @Nullable private String featureId;
  @Nullable private String featureTypeId;
  @Nullable private String formId;
  @Nullable private AuditInfoDoc created;
  @Nullable private AuditInfoDoc modified;
  @Nullable private Map<String, Object> responses;

  ObservationDocument(
      @Nullable String featureId,
      @Nullable String featureTypeId,
      @Nullable String formId,
      @Nullable AuditInfoDoc created,
      @Nullable AuditInfoDoc modified,
      @Nullable Map<String, Object> responses) {
    this.featureId = featureId;
    this.featureTypeId = featureTypeId;
    this.formId = formId;
    this.created = created;
    this.modified = modified;
    this.responses = responses;
  }

  @Nullable
  String getFeatureId() {
    return featureId;
  }

  @Nullable
  String getFeatureTypeId() {
    return featureTypeId;
  }

  @Nullable
  String getFormId() {
    return formId;
  }

  @Nullable
  AuditInfoDoc getCreated() {
    return created;
  }

  @Nullable
  AuditInfoDoc getModified() {
    return modified;
  }

  @Nullable
  Map<String, Object> getResponses() {
    return responses;
  }
}
