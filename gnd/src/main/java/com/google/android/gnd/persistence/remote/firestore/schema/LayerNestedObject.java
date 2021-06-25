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
import java.util.List;
import java.util.Map;

/** Firestore representation of map layers. */
@IgnoreExtraProperties
class LayerNestedObject {
  @Nullable private Map<String, String> name;
  // TODO(https://github.com/google/ground-platform/issues/402): Delete this field once updated in
  // web client.
  @Deprecated @Nullable private String color;
  @Nullable private StyleNestedObject defaultStyle;
  @Nullable private Map<String, FormNestedObject> forms;
  @Nullable private List<String> contributorsCanAdd;

  @SuppressWarnings("unused")
  public LayerNestedObject() {}

  @SuppressWarnings("unused")
  LayerNestedObject(
      @Nullable Map<String, String> name,
      @Nullable String color,
      @Nullable StyleNestedObject defaultStyle,
      @Nullable Map<String, FormNestedObject> forms,
      @Nullable List<String> contributorsCanAdd) {
    this.name = name;
    this.color = color;
    this.defaultStyle = defaultStyle;
    this.forms = forms;
    this.contributorsCanAdd = contributorsCanAdd;
  }

  @Nullable
  public Map<String, String> getName() {
    return name;
  }

  @Nullable
  @Deprecated
  public String getColor() {
    return color;
  }

  @Nullable
  public StyleNestedObject getDefaultStyle() {
    return defaultStyle;
  }

  @Nullable
  public Map<String, FormNestedObject> getForms() {
    return forms;
  }

  @Nullable
  public List<String> getContributorsCanAdd() {
    return contributorsCanAdd;
  }
}
