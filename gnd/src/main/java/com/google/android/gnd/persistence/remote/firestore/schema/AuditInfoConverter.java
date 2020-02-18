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
import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.User;
import com.google.firebase.Timestamp;
import java.util.Date;
import java8.util.Optional;

/** Converts between Firestore nested objects and {@link AuditInfo} instances. */
class AuditInfoConverter {

  /**
   * Converts a POJO representing user and timestamp data in Firebase into an equivalent model
   * object. This should never be empty in Firebase, but this method returns a default value when
   * the input is null to support legacy or corrupt dbs.
   */
  @NonNull
  static AuditInfo toAuditInfo(@Nullable AuditInfoNestedObject doc) {
    if (doc == null || doc.getClientTimeMillis() == null) {
      return AuditInfo.builder()
          .setUser(UserNestedObject.UNKNOWN_USER)
          .setClientTimeMillis(new Date(0))
          .build();
    }
    return AuditInfo.builder()
        .setUser(UserConverter.toUser(doc.getUser()))
        .setClientTimeMillis(doc.getClientTimeMillis().toDate())
        .setServerTimeMillis(Optional.ofNullable(doc.getServerTimeMillis()).map(Timestamp::toDate))
        .build();
  }

  @NonNull
  static AuditInfoNestedObject fromMutationAndUser(Mutation mutation, User user) {
    return new AuditInfoNestedObject(
        UserConverter.toNestedObject(user), new Timestamp(mutation.getClientTimestamp()), null);
  }
}
