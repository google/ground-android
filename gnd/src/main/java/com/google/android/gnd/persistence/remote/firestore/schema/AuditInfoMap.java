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
import static com.google.android.gnd.persistence.remote.firestore.base.FirestoreField.timestamp;

import androidx.annotation.NonNull;
import com.google.android.gnd.persistence.remote.firestore.base.FirestoreData;
import com.google.android.gnd.persistence.remote.firestore.base.FirestoreField;
import com.google.firebase.Timestamp;
import java.util.Map;
import java8.util.Optional;

/** User details and timestamp for creation or modification of a model object. */
public final class AuditInfoMap extends FirestoreData {
  private static final FirestoreField<UserMap> USER = nestedObject("user", UserMap.class);
  private static final FirestoreField<Timestamp> CLIENT_TIME_MILLIS = timestamp("clientTimeMillis");
  private static final FirestoreField<Timestamp> SERVER_TIME_MILLIS = timestamp("serverTimeMillis");

  public AuditInfoMap(Map<String, Object> map) {
    super(map);
  }

  public static Builder builder() {
    return new AuditInfoMap.Builder();
  }

  /**
   * The user initiating the related action. This should never be missing, but we handle missing
   * values anyway since the Firestore is schema-less.
   */
  @NonNull
  public Optional<UserMap> getUser() {
    return get(USER);
  }

  /**
   * The time at which the user action was initiated, according to the user's device. See {@link
   * System#currentTimeMillis} for details. This should never be missing, but we handle missing
   * values anyway since the Firestore is schema-less.
   */
  public Optional<Timestamp> getClientTimeMillis() {
    return get(CLIENT_TIME_MILLIS);
  }

  /**
   * The time at which the server received the requested change according to the server's internal
   * clock, or the updated server time was not yet received. See {@link System#currentTimeMillis}
   * for details. This will be missing until the server updates the write time and syncs it back to
   * the client.
   */
  public Optional<Timestamp> getServerTimeMillis() {
    return get(SERVER_TIME_MILLIS);
  }

  public static final class Builder extends FirestoreData.Builder<Builder> {
    private Builder() {
      super();
    }

    public Builder setUser(UserMap user) {
      return set(USER, user);
    }

    public Builder setClientTimeMillis(Timestamp clientTimeMillis) {
      return set(CLIENT_TIME_MILLIS, clientTimeMillis);
    }

    public Builder updateServerTimeMillis() {
      return updateServerTimestamp(SERVER_TIME_MILLIS);
    }

    public AuditInfoMap build() {
      return new AuditInfoMap(map());
    }
  }
}
