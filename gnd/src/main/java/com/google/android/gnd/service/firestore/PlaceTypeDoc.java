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

import static com.google.android.gnd.util.Localization.getLocalizedMessage;

import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.PlaceType;
import com.google.common.collect.ImmutableList;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.Map;

@IgnoreExtraProperties
public class PlaceTypeDoc {
  // TODO: Better name than pathKey? urlSubpath?
  public String pathKey;

  public Map<String, String> listHeading;

  public Map<String, String> itemLabel;

  public String iconId;

  public String iconColor;

  static PlaceType toProto(DocumentSnapshot doc, ImmutableList<Form> forms) {
    PlaceTypeDoc ft = doc.toObject(PlaceTypeDoc.class);
    return PlaceType.newBuilder()
                    .setId(doc.getId())
                    .setListHeading(getLocalizedMessage(ft.listHeading))
                    .setItemLabel(getLocalizedMessage(ft.itemLabel))
                    .setIconId(ft.iconId)
                    .setForms(forms)
                    .setIconColor(ft.iconColor)
                    .build();
  }
}
