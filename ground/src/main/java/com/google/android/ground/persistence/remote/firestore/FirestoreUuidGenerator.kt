/*
 * Copyright 2019 Google LLC
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

package com.google.android.ground.persistence.remote.firestore;

import static com.google.android.ground.persistence.remote.firestore.FirestoreDataStore.ID_COLLECTION;

import com.google.android.ground.persistence.uuid.OfflineUuidGenerator;
import com.google.firebase.firestore.FirebaseFirestore;
import javax.inject.Inject;

public class FirestoreUuidGenerator implements OfflineUuidGenerator {

  @Inject
  FirestoreUuidGenerator() {}

  @Override
  public String generateUuid() {
    return FirebaseFirestore.getInstance().collection(ID_COLLECTION).document().getId();
  }
}
