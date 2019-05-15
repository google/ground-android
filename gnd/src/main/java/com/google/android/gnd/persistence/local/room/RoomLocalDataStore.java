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

package com.google.android.gnd.persistence.local.room;

import androidx.room.Room;
import androidx.room.Transaction;
import com.google.android.gnd.GndApplication;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.shared.FeatureMutation;
import io.reactivex.Completable;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of local data store using Room ORM. Room abstracts persistence between a local db
 * and Java objects using a mix of inferred mappings based on Java field names and types, and custom
 * annotations. Mappings are defined through the various Entity objects in the package and related
 * embedded classes.
 */
@Singleton
public class RoomLocalDataStore implements LocalDataStore {
  private static final String DB_NAME = "gnd-db";

  private final LocalDatabase db;

  @Inject
  public RoomLocalDataStore(GndApplication app) {
    // TODO: Create db in module and inject DAOs directly.
    this.db =
        Room.databaseBuilder(app.getApplicationContext(), LocalDatabase.class, DB_NAME).build();
  }

  @Transaction
  @Override
  public Completable applyAndEnqueue(FeatureMutation mutation) {
    try {
      return apply(mutation).andThen(enqueue(mutation));
    } catch (LocalDataStoreException e) {
      return Completable.error(e);
    }
  }

  private Completable apply(FeatureMutation mutation) throws LocalDataStoreException {
    switch (mutation.getType()) {
      case CREATE:
        return db.featureDao().insert(FeatureEntity.fromMutation(mutation));
      default:
        throw LocalDataStoreException.unknownMutationType(mutation.getType());
    }
  }

  private Completable enqueue(FeatureMutation mutation) {
    return db.featureMutationDao().insert(FeatureMutationEntity.fromMutation(mutation));
  }
}
