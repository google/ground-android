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

/** Firestore representation of map feature styles. */
class StyleNestedObject {
  private String color;

  @SuppressWarnings("unused")
  public StyleNestedObject() {}

  @SuppressWarnings("unused")
  StyleNestedObject(String color) {
    this.color = color;
  }

  public String getColor() {
    return color;
  }
}
