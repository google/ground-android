/*
 * Copyright 2018 Google LLC
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

package com.google.android.ground.persistence.remote.firestore;

import static com.google.android.ground.util.Localization.getLocalizedMessage;

import androidx.annotation.Nullable;
import com.google.android.ground.model.Project;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.Map;
import java8.util.Maps;

@IgnoreExtraProperties
public class ProjectDoc {
  public static final String ACL = "acl";

  @Nullable public Map<String, String> title;

  @Nullable public Map<String, String> description;

  @Nullable public @ServerTimestamp Date serverTimeCreated;

  @Nullable public @ServerTimestamp Date serverTimeModified;

  @Nullable public Date clientTimeCreated;

  @Nullable public Date clientTimeModified;

  @Nullable public Map<String, LayerDoc> featureTypes;

  public static Project toObject(DocumentSnapshot doc) {
    ProjectDoc pd = doc.toObject(ProjectDoc.class);
    Project.Builder project = Project.newBuilder();
    project
        .setId(doc.getId())
        .setTitle(getLocalizedMessage(pd.title))
        .setDescription(getLocalizedMessage(pd.description));
    if (pd.featureTypes != null) {
      Maps.forEach(pd.featureTypes, (id, ptDoc) -> project.putLayer(id, ptDoc.toObject(id)));
    }
    return project.build();
  }
}
