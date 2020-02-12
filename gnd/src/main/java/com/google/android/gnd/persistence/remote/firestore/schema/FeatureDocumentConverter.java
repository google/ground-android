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

import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.persistence.remote.firestore.AuditInfoDoc;
import com.google.android.gnd.persistence.remote.firestore.DataStoreException;
import com.google.firebase.firestore.DocumentSnapshot;
import java8.util.Optional;

/** Converts between Firestore documents and {@link Feature} instances. */
class FeatureDocumentConverter {
  static Feature toFeature(Project project, DocumentSnapshot doc) {
    FeatureDocument f = doc.toObject(FeatureDocument.class);
    Optional<Layer> layer = project.getLayer(f.getFeatureTypeId());
    if (!layer.isPresent()) {
      throw new DataStoreException(
          "Unknown featureTypeId " + f.getFeatureTypeId() + " in feature " + doc.getId());
    }
    Point point =
        Point.newBuilder()
            .setLatitude(f.getCenter().getLatitude())
            .setLongitude(f.getCenter().getLongitude())
            .build();
    return Feature.newBuilder()
        .setId(doc.getId())
        .setProject(project)
        .setCustomId(f.getCustomId())
        .setCaption(f.getCaption())
        .setLayer(layer.get())
        .setPoint(point)
        .setCreated(AuditInfoDoc.toObject(f.getCreated()))
        .setLastModified(AuditInfoDoc.toObject(f.getModified()))
        .build();
  }
}
