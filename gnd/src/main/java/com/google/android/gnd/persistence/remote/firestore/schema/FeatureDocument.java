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
  @Nullable private String layerId;
  @Nullable private String customId;
  @Nullable private String caption;
  @Nullable private GeoPoint location;
  @Nullable private String geoJson;
  @Nullable private AuditInfoNestedObject created;
  @Nullable private AuditInfoNestedObject lastModified;

  @SuppressWarnings("unused")
  public FeatureDocument() {}

  @SuppressWarnings("unused")
  FeatureDocument(
      @Nullable String layerId,
      @Nullable String customId,
      @Nullable String caption,
      @Nullable GeoPoint location,
      @Nullable String geoJson,
      @Nullable AuditInfoNestedObject created,
      @Nullable AuditInfoNestedObject lastModified) {
    this.layerId = layerId;
    this.customId = customId;
    this.caption = caption;
    this.location = location;
    this.geoJson = geoJson;
    this.created = created;
    this.lastModified = lastModified;
  }

  @Nullable
  public String getLayerId() {
    return layerId;
  }

  @Nullable
  public String getCustomId() {
    return customId;
  }

  @Nullable
  public String getCaption() {
    return caption;
  }

  @Nullable
  public GeoPoint getLocation() {
    return location;
  }

  @Nullable
  public String getGeoJson() {
    return geoJson;
  }

  @Nullable
  public AuditInfoNestedObject getCreated() {
    return created;
  }

  @Nullable
  public AuditInfoNestedObject getLastModified() {
    return lastModified;
  }
}
