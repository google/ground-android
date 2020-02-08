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

package com.google.android.gnd.persistence.remote.firestore.converters;

import com.google.android.gnd.model.form.Option;
import com.google.android.gnd.persistence.remote.firestore.schema.OptionObject;

public final class OptionObjectConverter {

  public static Option toOption(OptionObject option) {
    Option.Builder builder = Option.newBuilder();
    option.getCode().ifPresent(builder::setCode);
    builder.setLabel(option.getLabels().getDefault());
    return builder.build();
  }
}
