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
import com.google.android.gnd.persistence.local.change.LocalChange;
import com.google.android.gnd.persistence.remote.RemoteChange;
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
  public Completable applyAndEnqueue(LocalChange localChange) {
    try {
      return apply(localChange).andThen(enqueue(localChange));
    } catch (LocalDataStoreException e) {
      return Completable.error(e);
    }
  }

  private Completable apply(LocalChange localChange) throws LocalDataStoreException {
    switch (localChange.getType()) {
      case CREATE_FEATURE:
        return db.featureDao()
            .insert(newFeatureEntity(localChange))
            .andThen(applyFeatureAttributeChanges(localChange));
      case UPDATE_FEATURE:
        // TODO: Implement.
      case DELETE_FEATURE:
        // TODO: Implement.
      case RELOAD_FEATURE:
        // TODO: Implement.
      case CREATE_RECORD:
        // TODO: Implement.
      case UPDATE_RECORD:
        // TODO: Implement.
      case DELETE_RECORD:
        // TODO: Implement.
      case RELOAD_RECORD:
        // TODO: Implement.
      default:
        throw new LocalDataStoreException("ChangeType." + localChange.getType());
    }
  }

  private Completable applyFeatureAttributeChanges(LocalChange localChange) {
    // TODO: Implement.
    return Completable.complete();
  }

  private FeatureEntity newFeatureEntity(LocalChange localChange) {
    return FeatureEntity.builder()
        .setId(localChange.getEntityId())
        .setProjectId(localChange.getProjectId())
        .setState(EntityState.DEFAULT)
        .build();
  }

  private Completable enqueue(LocalChange localChange) {
    // TODO: Implement.
    return Completable.error(new UnsupportedOperationException());
  }

  @Override
  public Single<ImmutableList<LocalChange>> getPendingChanges(String featureId) {
    // TODO: Implement.
    throw new UnsupportedOperationException();
  }

  @Transaction
  @Override
  public Completable dequeue(ImmutableList<LocalChange> localChanges) {
    // TODO: Implement.
    throw new UnsupportedOperationException();
  }

  @Transaction
  @Override
  public Completable markFailed(ImmutableList<LocalChange> localChanges) {
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
