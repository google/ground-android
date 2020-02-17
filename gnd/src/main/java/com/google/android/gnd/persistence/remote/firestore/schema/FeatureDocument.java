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
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.IgnoreExtraProperties;

/** Feature entity stored in Firestore. */
@IgnoreExtraProperties
class FeatureDocument {
  @Nullable private String featureTypeId;
  @Nullable private String customId;
  @Nullable private String caption;
  // TODO: Replace with consistent name throughout.
  @Nullable private GeoPoint center;
  @Nullable private AuditInfoNestedObject created;
  @Nullable private AuditInfoNestedObject modified;

  FeatureDocument(
      @Nullable String featureTypeId,
      @Nullable String customId,
      @Nullable String caption,
      @Nullable GeoPoint center,
      @Nullable AuditInfoNestedObject created,
      @Nullable AuditInfoNestedObject modified) {
    this.featureTypeId = featureTypeId;
    this.customId = customId;
    this.caption = caption;
    this.center = center;
    this.created = created;
    this.modified = modified;
  }

  @Nullable
  String getFeatureTypeId() {
    return featureTypeId;
  }

  @Nullable
  String getCustomId() {
    return customId;
  }

  @Nullable
  String getCaption() {
    return caption;
  }

  @Nullable
  GeoPoint getCenter() {
    return center;
  }

  @Nullable
  AuditInfoNestedObject getCreated() {
    return created;
  }

  @Nullable
  AuditInfoNestedObject getModified() {
    return modified;
  }
}
