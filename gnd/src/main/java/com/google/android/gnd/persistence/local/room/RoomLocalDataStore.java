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
import com.google.android.gnd.persistence.remote.RemoteChange;
import com.google.android.gnd.persistence.shared.FeatureMutation;
import com.google.android.gnd.persistence.shared.Mutation;
import com.google.android.gnd.persistence.shared.RecordMutation;
import com.google.common.collect.ImmutableList;
import io.reactivex.Completable;
import io.reactivex.Single;
import javax.inject.Inject;

public class RoomLocalDataStore implements LocalDataStore {
  private static final String DB_NAME = "gnd-db";

  private final LocalDatabase db;

  @Inject
  public RoomLocalDataStore(GndApplication app) {
    // TODO: Inject daos directly.
    this.db =
        Room.databaseBuilder(app.getApplicationContext(), LocalDatabase.class, DB_NAME).build();
  }

  @Transaction
  @Override
  public Completable applyAndEnqueue(Mutation mutation) {
    try {
      return apply(mutation).andThen(enqueue(mutation));
    } catch (LocalDataStoreException e) {
      return Completable.error(e);
    }
  }

  private Completable apply(Mutation mutation) throws LocalDataStoreException {
    if (mutation instanceof FeatureMutation) {
      return apply((FeatureMutation) mutation);
    } else if (mutation instanceof RecordMutation) {
      return apply((RecordMutation) mutation);
    } else {
      throw LocalDataStoreException.unknownMutationClass(mutation.getClass());
    }
  }

  private Completable apply(FeatureMutation mutation) throws LocalDataStoreException {
    switch (mutation.getType()) {
      case CREATE:
        return db.featureDao().insert(mutation.toNewFeatureEntity());
      case UPDATE:
        // TODO: Implement.
      case DELETE:
        // TODO: Implement.
      case RELOAD:
        // TODO: Implement.
      default:
        throw LocalDataStoreException.unknownMutationType(mutation.getType());
    }
  }

  private Completable apply(RecordMutation mutation) throws LocalDataStoreException {
    switch (mutation.getType()) {
      case CREATE:
        // TODO: Implement.
      case UPDATE:
        // TODO: Implement.
      case DELETE:
        // TODO: Implement.
      case RELOAD:
        // TODO: Implement.
      default:
        throw LocalDataStoreException.unknownMutationType(mutation.getType());
    }
  }

  private Completable enqueue(Mutation mutation) throws LocalDataStoreException {
    if (mutation instanceof FeatureMutation) {
      return enqueue((FeatureMutation) mutation);
    } else if (mutation instanceof RecordMutation) {
      return enqueue((RecordMutation) mutation);
    } else {
      throw LocalDataStoreException.unknownMutationClass(mutation.getClass());
    }
  }

  private Completable enqueue(FeatureMutation mutation) throws LocalDataStoreException {
    // TODO: Implement.
    return Completable.complete();
  }

  private Completable enqueue(RecordMutation mutation) throws LocalDataStoreException {
    // TODO: Implement.
    return Completable.complete();
  }

  @Override
  public Single<ImmutableList<Mutation>> getPendingChanges(String featureId) {
    // TODO: Implement.
    throw new UnsupportedOperationException();
  }

  @Transaction
  @Override
  public Completable dequeue(ImmutableList<Mutation> mutations) {
    // TODO: Implement.
    throw new UnsupportedOperationException();
  }

  @Transaction
  @Override
  public Completable markFailed(ImmutableList<Mutation> mutations) {
    // TODO: Implement.
    throw new UnsupportedOperationException();
  }

  @Transaction
  @Override
  public Completable merge(RemoteChange<?> remoteChange) {
    // TODO: Implement.
    throw new UnsupportedOperationException();
  }
}
