/*
 *
 *  * Copyright 2020 Google LLC
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     https://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.google.android.gnd.persistence.remote.firestore.converters;

import static com.google.android.gnd.persistence.remote.firestore.base.FirestoreField.object;
import static com.google.android.gnd.persistence.remote.firestore.base.FirestoreField.timestamp;

import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.User;
import com.google.android.gnd.persistence.remote.firestore.UserData;
import com.google.android.gnd.persistence.remote.firestore.base.FirestoreField;
import com.google.firebase.Timestamp;
import java.util.Map;

public class AuditInfoData {
  public static final FirestoreField<Timestamp> CLIENT_TIME_MILLIS = timestamp("clientTimeMillis");
  public static final FirestoreField<Timestamp> SERVER_TIME_MILLIS = timestamp("serverTimeMillis");
  public static final FirestoreField<UserData> USER = object("user", UserData.class);

  AuditInfoData() {}

  AuditInfoData(Map<String, Object> data) {
    super(data);
  }

  public AuditInfo toAuditInfo() {
    return AuditInfo.builder()
        .setClientTimeMillis(getRequired(CLIENT_TIME_MILLIS).toDate())
        .setServerTimeMillis(get(SERVER_TIME_MILLIS).map(Timestamp::toDate))
        .setUser(getRequired(USER).toUser())
        .build();
  }

  public static AuditInfoData fromMutation(Mutation mutation, User user) {
    AuditInfoData auditInfo = new AuditInfoData();
    auditInfo.set(AuditInfoData.USER, UserData.fromUser(user));
    auditInfo.set(AuditInfoData.CLIENT_TIME_MILLIS, new Timestamp(mutation.getClientTimestamp()));
    return auditInfo;
  }
}
