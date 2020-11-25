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

import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Form;
import com.google.common.collect.ImmutableList;
import java.util.Map;

/** Converts between Firestore nested objects and {@link Form} instances. */
class FormConverter {

  static Form toForm(String formId, FormNestedObject obj) {
    return Form.newBuilder().setId(formId).setElements(toList(obj.getElements())).build();
  }

  private static ImmutableList<Element> toList(Map<String, ElementNestedObject> elements) {
    return stream(elements.entrySet())
        .map(e -> ElementConverter.toElement(e.getKey(), e.getValue()))
        .collect(toImmutableList());
  }
}
