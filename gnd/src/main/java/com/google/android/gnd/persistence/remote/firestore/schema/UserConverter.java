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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gnd.model.User;

/** Converts between Firestore objects and {@link User} instances. */
class UserConverter {

  @NonNull
  static UserNestedObject toNestedObject(@NonNull User user) {
    return new UserNestedObject(user.getId(), user.getEmail(), user.getDisplayName());
  }

  @NonNull
  static User toUser(@Nullable UserNestedObject ud) {
    if (ud == null || ud.getId() == null || ud.getEmail() == null || ud.getDisplayName() == null) {
      // Degrade gracefully when user info missing in remote db.
      ud = UserNestedObject.UNKNOWN_USER;
    }
    return User.builder()
        .setId(ud.getId())
        .setEmail(ud.getEmail())
        .setDisplayName(ud.getDisplayName())
        .build();
  }
}
