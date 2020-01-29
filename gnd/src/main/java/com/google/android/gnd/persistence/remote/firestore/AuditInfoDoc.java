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

package com.google.android.gnd.persistence.remote.firestore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.User;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java8.util.Optional;

/** User details and timestamp for creation or modification of a model object. */
public class AuditInfoDoc {

  /**
   * The user initiating the related action. This should never be missing, but we handle null values
   * anyway since the Firestore is schema-less.
   */
  @Nullable public UserDoc user;

  /**
   * The time at which the user action was initiated, according to the user's device. See {@link
   * System#currentTimeMillis} for details. This should never be missing, but we handle null values
   * anyway since the Firestore is schema-less.
   */
  @Nullable public Timestamp clientTimeMillis;

  /**
   * The time at which the server received the requested change according to the server's internal
   * clock, or the updated server time was not yet received. See {@link System#currentTimeMillis}
   * for details. This will be null until the server updates the write time and syncs it back to the
   * client.
   */
  @Nullable @ServerTimestamp public Timestamp serverTimeMillis;

  /**
   * Converts a POJO representing user and timestamp data in Firebase into an equivalent model
   * object. This should never be empty in Firebase, but this method returns a default value when
   * the input is null to support legacy or corrupt dbs.
   */
  @NonNull
  public static AuditInfo toObject(@Nullable AuditInfoDoc doc) {
    if (doc == null || doc.clientTimeMillis == null) {
      return AuditInfo.builder()
          .setUser(UserDoc.UNKNOWN_USER)
          .setClientTimeMillis(new Date(0))
          .build();
    }
    return AuditInfo.builder()
        .setUser(UserDoc.toObject(doc.user))
        .setClientTimeMillis(doc.clientTimeMillis.toDate())
        .setServerTimeMillis(Optional.ofNullable(doc.serverTimeMillis).map(Timestamp::toDate))
        .build();
  }

  public static AuditInfoDoc fromMutationAndUser(Mutation mutation, User user) {
    AuditInfoDoc auditInfo = new AuditInfoDoc();
    auditInfo.user = UserDoc.fromObject(user);
    auditInfo.clientTimeMillis = new Timestamp(mutation.getClientTimestamp());
    return auditInfo;
  }
}
