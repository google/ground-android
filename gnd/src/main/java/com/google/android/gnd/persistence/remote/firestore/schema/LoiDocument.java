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
import java.util.Map;

/** LOI entity stored in Firestore. */
@IgnoreExtraProperties
class LoiDocument {
  @Nullable private String jobId;
  @Nullable private String customId;
  @Nullable private String caption;
  @Nullable private GeoPoint location;
  // TODO(#929): Harmonize representation of points, polygons, and geometries in remote db.
  @Nullable private String geoJson;
  @Nullable private Map<String, Object> geometry;
  @Nullable private AuditInfoNestedObject created;
  @Nullable private AuditInfoNestedObject lastModified;

  @SuppressWarnings("unused")
  public LoiDocument() {}

  @SuppressWarnings("unused")
  LoiDocument(
      @Nullable String jobId,
      @Nullable String customId,
      @Nullable String caption,
      @Nullable GeoPoint location,
      @Nullable String geoJson,
      @Nullable Map<String, Object> geometry,
      @Nullable AuditInfoNestedObject created,
      @Nullable AuditInfoNestedObject lastModified) {
    this.jobId = jobId;
    this.customId = customId;
    this.caption = caption;
    this.location = location;
    this.geoJson = geoJson;
    this.geometry = geometry;
    this.created = created;
    this.lastModified = lastModified;
  }

  @Nullable
  public String getJobId() {
    return jobId;
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
  public Map<String, Object> getGeometry() {
    return geometry;
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
