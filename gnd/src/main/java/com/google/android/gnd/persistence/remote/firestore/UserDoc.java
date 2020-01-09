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
import com.google.android.gnd.model.User;

public class UserDoc {
  @Nullable public String id;
  @Nullable public String email;
  @Nullable public String displayName;

  /** Fallback value when reading invalid or legacy schemas. */
  public static final User UNKNOWN_USER =
      User.builder().setId("").setEmail("").setDisplayName("Unknown user").build();

  @NonNull
  public static UserDoc fromObject(@NonNull User user) {
    UserDoc ud = new UserDoc();
    ud.id = user.getId();
    ud.email = user.getEmail();
    ud.displayName = user.getDisplayName();
    return ud;
  }

  @NonNull
  public static User toObject(@Nullable UserDoc ud) {
    if (ud == null || ud.id == null || ud.email == null || ud.displayName == null) {
      return UNKNOWN_USER;
    }
    return User.builder().setId(ud.id).setEmail(ud.email).setDisplayName(ud.displayName).build();
  }
}
