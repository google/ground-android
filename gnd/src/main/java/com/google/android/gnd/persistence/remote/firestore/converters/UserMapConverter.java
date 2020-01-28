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

import static com.google.android.gnd.persistence.remote.firestore.schema.UserMapFields.DISPLAY_NAME;
import static com.google.android.gnd.persistence.remote.firestore.schema.UserMapFields.EMAIL;
import static com.google.android.gnd.persistence.remote.firestore.schema.UserMapFields.ID;

import androidx.annotation.NonNull;
import com.google.android.gnd.model.User;
import com.google.android.gnd.persistence.remote.firestore.DataStoreException;
import com.google.android.gnd.persistence.remote.firestore.base.Data;

/** Converts between user details nested inside Firestore documents and equivalent model objects. */
public class UserMapConverter {

  /** Fallback value when reading invalid or legacy schemas. */
  public static final User UNKNOWN_USER =
      User.builder().setId("").setEmail("").setDisplayName("Unknown user").build();

  @NonNull
  public static Data fromUser(@NonNull User user) {
    return Data.builder()
        .set(ID, user.getId())
        .set(EMAIL, user.getEmail())
        .set(DISPLAY_NAME, user.getDisplayName())
        .build();
  }

  @NonNull
  public static User toUser(@NonNull Data data) {
    try {
      return User.builder()
          .setId(data.getRequired(ID))
          .setEmail(data.getRequired(EMAIL))
          .setDisplayName(data.getRequired(DISPLAY_NAME))
          .build();
    } catch (DataStoreException e) {
      return UNKNOWN_USER;
    }
  }
}
