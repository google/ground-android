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

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.android.gnd.model.FeatureType;
import com.google.android.gnd.model.Form;

import java.util.List;
import java.util.Map;

@IgnoreExtraProperties
public class FeatureTypeDoc {
  // TODO: Better name than pathKey? urlSubpath?
  public String pathKey;

  public Map<String, String> listHeading;

  public Map<String, String> itemLabel;

  public String iconId;

  public String iconColor;

  static FeatureType toProto(DocumentSnapshot doc, List<Form> forms) {
    FeatureTypeDoc ft = doc.toObject(FeatureTypeDoc.class);
    return FeatureType.newBuilder()
        .setId(doc.getId())
        .putAllListHeading(ft.listHeading)
        .putAllItemLabel(ft.itemLabel)
        .setUrlSubpath(ft.pathKey)
        .setIconId(ft.iconId)
        .addAllForms(forms)
        .setIconColor(ft.iconColor)
        .build();
  }
}
