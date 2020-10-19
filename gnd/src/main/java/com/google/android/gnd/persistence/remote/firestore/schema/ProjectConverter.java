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

import androidx.annotation.NonNull;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.basemap.OfflineBaseMapSource;
import com.google.android.gnd.persistence.remote.DataStoreException;
import com.google.firebase.firestore.DocumentSnapshot;
import java.net.MalformedURLException;
import java.net.URL;
import java8.util.Maps;
import timber.log.Timber;

/** Converts between Firestore documents and {@link Project} instances. */
class ProjectConverter {

  @NonNull
  static Project toProject(@NonNull DocumentSnapshot doc) throws DataStoreException {
    ProjectDocument pd = doc.toObject(ProjectDocument.class);
    Project.Builder project = Project.newBuilder();
    project
        .setId(doc.getId())
        .setTitle(getLocalizedMessage(pd.getTitle()))
        .setDescription(getLocalizedMessage(pd.getDescription()));
    if (pd.getLayers() != null) {
      Maps.forEach(
          pd.getLayers(), (id, obj) -> project.putLayer(id, LayerConverter.toLayer(id, obj)));
    }
    if (pd.getOfflineBaseMapSources() != null) {
      convertOfflineBaseMapSources(pd, project);
    }
    return project.build();
  }

  private static void convertOfflineBaseMapSources(@NonNull ProjectDocument pd, @NonNull Project.Builder project) {
    for (OfflineBaseMapSourceNestedObject src : pd.getOfflineBaseMapSources()) {
      if (src.getUrl() == null) {
        Timber.d("Skipping base map source in project with missing URL");
        continue;
      }
      try {
        URL url = new URL(src.getUrl());
        project.addOfflineBaseMapSource(OfflineBaseMapSource.builder().setUrl(url).build());
      } catch (MalformedURLException e) {
        Timber.d("Skipping base map source in project with malformed URL");
      }
    }
  }
}
