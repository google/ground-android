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

/** Firestore representation of form element definitions. */
@IgnoreExtraProperties
class ElementNestedObject {
  @Nullable private String id;
  @Nullable private String type;
  @Nullable private String cardinality;
  // TODO: labels or label?
  @Nullable private Map<String, String> labels;
  @Nullable private List<OptionNestedObject> options;
  @Nullable private Boolean required;

  ElementNestedObject(
      @Nullable String id,
      @Nullable String type,
      @Nullable String cardinality,
      @Nullable Map<String, String> labels,
      @Nullable List<OptionNestedObject> options,
      @Nullable Boolean required) {
    this.id = id;
    this.type = type;
    this.cardinality = cardinality;
    this.labels = labels;
    this.options = options;
    this.required = required;
  }

  @Nullable
  String getId() {
    return id;
  }

  @Nullable
  String getType() {
    return type;
  }

  @Nullable
  String getCardinality() {
    return cardinality;
  }

  @Nullable
  Map<String, String> getLabels() {
    return labels;
  }

  @Nullable
  List<OptionNestedObject> getOptions() {
    return options;
  }

  @Nullable
  Boolean getRequired() {
    return required;
  }
}
