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

/** Project entity stored in Firestore. */
@IgnoreExtraProperties
class ProjectDocument {
  @Nullable private Map<String, String> title;

  @Nullable private Map<String, String> description;

  // TODO: Add AuditInfoDoc for created and lastModified.

  // TODO: Rename to "layers" once db is migrated.

  @Nullable private Map<String, LayerNestedObject> featureTypes;

  ProjectDocument(
      @Nullable Map<String, String> title,
      @Nullable Map<String, String> description,
      @Nullable Map<String, LayerNestedObject> featureTypes) {
    this.title = title;
    this.description = description;
    this.featureTypes = featureTypes;
  }

  @Nullable
  Map<String, String> getTitle() {
    return title;
  }

  @Nullable
  Map<String, String> getDescription() {
    return description;
  }

  @Nullable
  Map<String, LayerNestedObject> getFeatureTypes() {
    return featureTypes;
  }
}
