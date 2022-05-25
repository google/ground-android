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

/**
 * Firestore representation of task element definitions.
 */
@IgnoreExtraProperties
class ElementNestedObject {

  @Nullable
  private Integer index;
  @Nullable
  private String type;
  @Nullable
  private String cardinality;
  @Nullable
  private Map<String, String> label;
  @Nullable
  private Map<String, OptionNestedObject> options;
  @Nullable
  private Boolean required;

  @SuppressWarnings("unused")
  public ElementNestedObject() {
  }

  @SuppressWarnings("unused")
  ElementNestedObject(
      @Nullable Integer index,
      @Nullable String type,
      @Nullable String cardinality,
      @Nullable Map<String, String> label,
      @Nullable Map<String, OptionNestedObject> options,
      @Nullable Boolean required) {
    this.index = index;
    this.type = type;
    this.cardinality = cardinality;
    this.label = label;
    this.options = options;
    this.required = required;
  }

  @Nullable
  public Integer getIndex() {
    return index;
  }

  @Nullable
  public String getType() {
    return type;
  }

  @Nullable
  public String getCardinality() {
    return cardinality;
  }

  @Nullable
  public Map<String, String> getLabel() {
    return label;
  }

  @Nullable
  public Map<String, OptionNestedObject> getOptions() {
    return options;
  }

  @Nullable
  public Boolean getRequired() {
    return required;
  }
}
