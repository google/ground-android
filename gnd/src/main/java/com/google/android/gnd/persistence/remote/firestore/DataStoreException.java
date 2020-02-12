/*
 * Copyright 2018 Google LLC
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

package com.google.android.gnd.persistence.remote.firestore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java8.util.Optional;

public class DataStoreException extends RuntimeException {
  public DataStoreException(String message) {
    super(message);
  }

  @NonNull
  public static <T> T checkNotNull(@Nullable T reference, String field) throws DataStoreException {
    if (reference == null) {
      throw new DataStoreException("Missing " + field);
    }
    return reference;
  }

  @NonNull
  public static <T> T checkNotEmpty(@NonNull Optional<T> optional, @NonNull String field)
      throws DataStoreException {
    return optional.orElseThrow(() -> new DataStoreException("Missing " + field));
  }
}
