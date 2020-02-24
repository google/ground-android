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
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.Map;

/** Firestore representation of map layers. */
@IgnoreExtraProperties
class LayerNestedObject {
  @Nullable private Map<String, String> listHeading;
  @Nullable private Map<String, String> itemLabel;
  @Nullable private StyleNestedObject defaultStyle;
  @Nullable private Map<String, FormNestedObject> forms;

  @SuppressWarnings("unused")
  public LayerNestedObject() {}

  @SuppressWarnings("unused")
  LayerNestedObject(
      @Nullable Map<String, String> listHeading,
      @Nullable Map<String, String> itemLabel,
      @Nullable StyleNestedObject defaultStyle,
      @Nullable Map<String, FormNestedObject> forms) {
    this.listHeading = listHeading;
    this.itemLabel = itemLabel;
    this.defaultStyle = defaultStyle;
    this.forms = forms;
  }

  @Nullable
  public Map<String, String> getListHeading() {
    return listHeading;
  }

  @Nullable
  public Map<String, String> getItemLabel() {
    return itemLabel;
  }

  @Nullable
  public StyleNestedObject getDefaultStyle() {
    return defaultStyle;
  }

  @Nullable
  public Map<String, FormNestedObject> getForms() {
    return forms;
  }
}
