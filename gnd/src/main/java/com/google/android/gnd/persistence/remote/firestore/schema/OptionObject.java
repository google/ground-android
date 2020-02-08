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

import static com.google.android.gnd.persistence.remote.firestore.base.FirestoreField.nestedObject;
import static com.google.android.gnd.persistence.remote.firestore.base.FirestoreField.string;

import com.google.android.gnd.persistence.remote.firestore.base.FirestoreData;
import com.google.android.gnd.persistence.remote.firestore.base.FirestoreField;
import java.util.Map;
import java8.util.Optional;

public class OptionObject extends FirestoreData {
  private static final FirestoreField<String> CODE = string("code");
  private static final FirestoreField<StringMap> LABELS = nestedObject("labels", StringMap.class);

  public OptionObject(Map<String, Object> map) {
    super(map);
  }

  public Optional<String> getCode() {
    return get(CODE);
  }

  public StringMap getLabels() {
    return get(LABELS).orElse(StringMap.emptyMap());
  }
}
