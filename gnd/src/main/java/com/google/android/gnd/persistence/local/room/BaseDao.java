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

import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Update;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.List;

/**
 * Base interface for DAOs that implement operations on a specific entity type.
 *
 * @param <E> the type of entity that is persisted by sub-interfaces.
 */
public interface BaseDao<E> {

  @Insert
  Completable insert(E entity);

  @Update
  Single<Integer> update(E entity);

  @Update
  Completable updateAll(List<E> entities);

  @Delete
  Completable delete(E entity);

  /** Try to update the specified entity, and if it doesn't yet exist, create it. */
  default Completable insertOrUpdate(E entity) {
    return update(entity).filter(n -> n == 0).flatMapCompletable(__ -> insert(entity));
  }
}
