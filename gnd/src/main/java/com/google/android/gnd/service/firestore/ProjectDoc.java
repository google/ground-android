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

package com.google.android.gnd.service.firestore;

import com.google.android.gnd.model.PlaceType;
import com.google.android.gnd.model.Project;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@IgnoreExtraProperties
public class ProjectDoc {
  public Map<String, String> title;

  public Map<String, String> description;

  public @ServerTimestamp Date serverTimeCreated;

  public @ServerTimestamp Date serverTimeModified;

  public Date clientTimeCreated;

  public Date clientTimeModified;

  public static Project toProto(DocumentSnapshot doc, List<PlaceType> placeTypes) {
    ProjectDoc pd = doc.toObject(ProjectDoc.class);
    return Project.newBuilder()
        .setId(doc.getId())
        .putAllTitle(pd.title)
        .putAllDescription(pd.description)
        .addAllPlaceTypes(placeTypes)
        .build();
  }

  public static Project toProto(DocumentSnapshot doc) {
    return toProto(doc, Collections.emptyList());
  }
}
