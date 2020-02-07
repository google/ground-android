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

import com.google.android.gnd.persistence.remote.firestore.base.FirestoreData;
import com.google.android.gnd.persistence.remote.firestore.base.FirestoreField;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java8.util.Optional;

/** Represents user info stored as a nested object inside Firestore documents. */
public final class UserObject extends FirestoreData {
  private static final FirestoreField<String> ID = FirestoreField.string("id");
  private static final FirestoreField<String> EMAIL = FirestoreField.string("email");
  private static final FirestoreField<String> DISPLAY_NAME = FirestoreField.string("displayName");

  private UserObject(ImmutableMap<String, Object> map) {
    super(map);
  }

  public static UserObject fromMap(Map<String, Object> map) {
    return new UserObject(ImmutableMap.copyOf(map));
  }

  public static UserObject.Builder builder() {
    return new Builder();
  }

  public Optional<String> getId() {
    return get(ID);
  }

  public Optional<String> getEmail() {
    return get(EMAIL);
  }

  public Optional<String> getDisplayName() {
    return get(DISPLAY_NAME);
  }

  public static final class Builder extends FirestoreData.Builder<Builder> {
    private Builder() {
      super();
    }

    public Builder setId(String id) {
      return set(ID, id);
    }

    public Builder setEmail(String email) {
      return set(EMAIL, email);
    }

    public Builder setDisplayName(String displayName) {
      return set(DISPLAY_NAME, displayName);
    }

    public UserObject build() {
      return new UserObject(toMap());
    }
  }
}
