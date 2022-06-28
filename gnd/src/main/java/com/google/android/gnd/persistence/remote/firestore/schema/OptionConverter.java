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

import static com.google.android.gnd.util.Localization.getLocalizedMessage;

import com.google.android.gnd.model.task.Option;

/**
 * Converts between Firestore nested objects and {@link Option} instances.
 */
class OptionConverter {

  static Option toOption(String id, OptionNestedObject option) {
    Option.Builder builder = Option.newBuilder();
    builder.setId(id);
    if (option.getCode() != null) {
      builder.setCode(option.getCode());
    }
    if (option.getLabel() != null) {
      builder.setLabel(getLocalizedMessage(option.getLabel()));
    }
    return builder.build();
  }
}
