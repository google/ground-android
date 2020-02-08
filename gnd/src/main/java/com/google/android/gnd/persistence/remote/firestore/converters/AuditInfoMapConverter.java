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
import androidx.annotation.Nullable;
import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.User;
import com.google.android.gnd.persistence.remote.firestore.schema.AuditInfoMap;
import com.google.firebase.Timestamp;
import java.util.Date;

public class AuditInfoMapConverter {
  /**
   * Converts a user and timestamp data nested in Firebase documents into an equivalent model
   * object. This should never be empty in the remote data store, but this method returns a default
   * value when the input is missing to support debugging and dbs migrations.
   */
  @NonNull
  public static AuditInfo toAuditInfo(@Nullable AuditInfoMap doc) {
    if (doc == null || doc.getClientTimeMillis().isEmpty()) {
      return AuditInfo.builder()
          .setUser(UserMapConverter.UNKNOWN_USER)
          .setClientTimeMillis(new Date(0))
          .build();
    }
    return AuditInfo.builder()
        .setUser(UserMapConverter.toUser(doc.getUser()))
        .setClientTimeMillis(doc.getClientTimeMillis().get().toDate())
        .setServerTimeMillis(doc.getServerTimeMillis().map(Timestamp::toDate))
        .build();
  }

  public static AuditInfoMap fromMutationAndUser(Mutation mutation, User user) {
    return AuditInfoMap.builder()
        .setUser(UserMapConverter.fromUser(user))
        .setClientTimeMillis(new Timestamp(mutation.getClientTimestamp()))
        .updateServerTimeMillis()
        .build();
  }
}
