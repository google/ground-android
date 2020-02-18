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

import static com.google.android.gnd.util.Localization.getLocalizedMessage;

import com.google.android.gnd.model.Project;
import com.google.android.gnd.persistence.remote.DataStoreException;
import com.google.firebase.firestore.DocumentSnapshot;
import java8.util.Maps;

/** Converts between Firestore documents and {@link Project} instances. */
class ProjectConverter {

  static Project toProject(DocumentSnapshot doc) throws DataStoreException {
    ProjectDocument pd = doc.toObject(ProjectDocument.class);
    Project.Builder project = Project.newBuilder();
    project
        .setId(doc.getId())
        .setTitle(getLocalizedMessage(pd.getTitle()))
        .setDescription(getLocalizedMessage(pd.getDescription()));
    if (pd.getFeatureTypes() != null) {
      Maps.forEach(
          pd.getFeatureTypes(), (id, obj) -> project.putLayer(id, LayerConverter.toLayer(id, obj)));
    }
    return project.build();
  }
}
