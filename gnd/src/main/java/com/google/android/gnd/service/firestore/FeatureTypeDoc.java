/*
 * Copyright 2019 Google LLC
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

import static com.google.android.gnd.util.Localization.getLocalizedMessage;

import androidx.annotation.Nullable;
import com.google.android.gnd.vo.FeatureType;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.Map;
import java8.util.Maps;

@IgnoreExtraProperties
public class FeatureTypeDoc {
  // TODO: Better name than pathKey? urlSubpath?
  @Nullable public String pathKey;

  @Nullable public Map<String, String> listHeading;

  @Nullable public Map<String, String> itemLabel;

  @Nullable public String iconId;

  @Nullable public String iconColor;

  @Nullable public Map<String, FormDoc> forms;

  public FeatureType toObject(String id) {
    FeatureType.Builder featureType = FeatureType.newBuilder();
    featureType
        .setId(id)
        .setListHeading(getLocalizedMessage(listHeading))
        .setItemLabel(getLocalizedMessage(itemLabel))
        .setIconId(iconId)
        .setIconColor(iconColor);
    if (forms != null) {
      Maps.forEach(forms, (formId, formDoc) -> featureType.addForm(formDoc.toObject(formId)));
    }
    return featureType.build();
  }
}
