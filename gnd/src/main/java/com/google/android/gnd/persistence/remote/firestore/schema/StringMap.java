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

import static com.google.android.gnd.persistence.remote.firestore.base.FirestoreField.string;

import androidx.annotation.NonNull;
import com.google.android.gnd.persistence.remote.firestore.base.FirestoreData;
import java.util.Collections;
import java.util.Map;
import java8.util.Optional;

/**
 * Wraps a map of language codes to Strings nested in a Firestore documents. In Ground, this is used
 * to represent user defined multilingual strings.
 */
public final class StringMap extends FirestoreData {

  protected StringMap(Map<String, Object> map) {
    super(map);
  }

  public static StringMap emptyMap() {
    return new StringMap(Collections.emptyMap());
  }

  public Optional<String> get(String lang) {
    return super.get(string(lang));
  }

  /**
   * Accessor that looks through multilingual strings and returns whatever it can file. This is a
   * temporary solution until multingual support is completed.
   */
  @NonNull
  public String getDefault() {
    // TODO: Allow user to select language when joining project and use that one.
    // TODO: Return Optional and handle empty strings in client code.
    return get("_").orElseGet(() -> get("en").orElseGet(() -> get("pt").orElse("<Untitled>")));
  }

  public static final class Builder extends FirestoreData.Builder<Builder> {

    protected Builder set(String lang, String text) {
      return set(string(lang), text);
    }
  }
}
