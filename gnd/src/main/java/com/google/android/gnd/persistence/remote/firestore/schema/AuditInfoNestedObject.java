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

import androidx.annotation.Nullable;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

/** User details and timestamp for creation or modification of a model object. */
class AuditInfoNestedObject {
  @Nullable private UserNestedObject user;
  @Nullable private Timestamp clientTimeMillis;
  @Nullable @ServerTimestamp private Timestamp serverTimeMillis;

  @SuppressWarnings("unused")
  public AuditInfoNestedObject() {}

  AuditInfoNestedObject(
      @Nullable UserNestedObject user,
      @Nullable Timestamp clientTimeMillis,
      @Nullable Timestamp serverTimeMillis) {
    this.user = user;
    this.clientTimeMillis = clientTimeMillis;
    this.serverTimeMillis = serverTimeMillis;
  }

  /**
   * The user initiating the related action. This should never be missing, but we handle null values
   * anyway since the Firestore is schema-less.
   */
  @Nullable
  public UserNestedObject getUser() {
    return user;
  }

  /**
   * The time at which the user action was initiated, according to the user's device. See {@link
   * System#currentTimeMillis} for details. This should never be missing, but we handle null values
   * anyway since the Firestore is schema-less.
   */
  @Nullable
  public Timestamp getClientTimeMillis() {
    return clientTimeMillis;
  }

  /**
   * The time at which the server received the requested change according to the server's internal
   * clock, or the updated server time was not yet received. See {@link System#currentTimeMillis}
   * for details. This will be null until the server updates the write time and syncs it back to the
   * client.
   */
  @Nullable
  public Timestamp getServerTimeMillis() {
    return serverTimeMillis;
  }
}
