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
import com.google.android.ground.model.layer.Layer;
import com.google.android.ground.model.layer.Style;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.Map;
import java8.util.Maps;

@IgnoreExtraProperties
public class LayerDoc {

  @Nullable public Map<String, String> name;

  @Nullable public StyleDoc defaultStyle;

  @Nullable public Map<String, FormDoc> forms;

  public Layer toObject(String id) {
    Layer.Builder layer = Layer.newBuilder();
    layer.setId(id).setName(getLocalizedMessage(name));
    if (defaultStyle != null) {
      layer.setDefaultStyle(defaultStyle.toObject());
    }
    if (forms != null) {
      Maps.forEach(forms, (formId, formDoc) -> layer.addForm(formDoc.toObject(formId)));
    }
    return layer.build();
  }

  public static class StyleDoc {
    @Nullable public String color;

    public Style toObject() {
      return Style.builder().setColor(color).build();
    }
  }
}
