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

import androidx.annotation.NonNull;
import com.google.android.gnd.model.User;
import com.google.android.gnd.persistence.remote.firestore.schema.UserMap;
import java.util.NoSuchElementException;
import java8.util.Optional;

/** Converts between user details nested inside Firestore documents and equivalent model objects. */
public class UserMapConverter {

  /** Fallback value when reading invalid or legacy schemas. */
  public static final User UNKNOWN_USER =
      User.builder().setId("").setEmail("").setDisplayName("Unknown user").build();

  @NonNull
  public static UserMap fromUser(@NonNull User user) {
    return UserMap.builder()
        .setId(user.getId())
        .setEmail(user.getEmail())
        .setDisplayName(user.getDisplayName())
        .build();
  }

  @NonNull
  public static User toUser(@NonNull Optional<UserMap> data) {
    return data.map(UserMapConverter::toUser).orElse(UNKNOWN_USER);
  }

  @NonNull
  public static User toUser(@NonNull UserMap data) {
    try {
      return User.builder()
          .setId(data.getId().get())
          .setEmail(data.getEmail().get())
          .setDisplayName(data.getDisplayName().get())
          .build();
    } catch (NoSuchElementException e) {
      return UNKNOWN_USER;
    }
  }
}
