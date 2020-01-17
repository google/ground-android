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

import static com.google.android.gnd.persistence.remote.firestore.schema.FeatureDocumentReference.CREATED;
import static com.google.android.gnd.persistence.remote.firestore.schema.FeatureDocumentReference.LAST_MODIFIED;
import static com.google.android.gnd.persistence.remote.firestore.schema.FeatureDocumentReference.LAYER_ID;
import static com.google.android.gnd.persistence.remote.firestore.schema.FeatureDocumentReference.LOCATION;

import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.persistence.remote.firestore.base.FirestoreData;
import com.google.common.collect.ImmutableMap;

public class FeatureMutationConverter {

  public static ImmutableMap<String, Object> toFirestoreData(FeatureMutation mutation, User user) {
    FirestoreData data = new FirestoreData();
    data.set(LAYER_ID, mutation.getLayerId());
    mutation.getNewLocation().map(PointConverter::toGeoPoint).ifPresent(p -> data.set(LOCATION, p));
    AuditInfoData auditInfo = AuditInfoData.fromMutation(mutation, user);
    switch (mutation.getType()) {
      case CREATE:
        data.set(CREATED, auditInfo);
        data.set(LAST_MODIFIED, auditInfo);
        break;
      case UPDATE:
      case DELETE:
      case UNKNOWN:
        // TODO.
        throw new UnsupportedOperationException();
    }
    return data.getData();
  }
}
