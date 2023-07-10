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
package com.google.android.ground.persistence.local.room.dao

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update
import io.reactivex.Completable
import io.reactivex.Single

/**
 * Base interface for DAOs that implement operations on a specific entity type.
 *
 * @param <E> the type of entity that is persisted by sub-interfaces. </E>
 */
interface BaseDao<E> {
  @Deprecated("Use insertSuspend instead") @Insert fun insert(entity: E): Completable

  // TODO(#1581): Rename once all uses migrated to coroutines.
  /** Insert entity into local db. Main-safe. */
  @Insert suspend fun insertSuspend(entity: E)

  /** Update entity in local db. Main-safe. */
  @Deprecated("Use updateSuspend instead") @Update fun update(entity: E): Single<Int>

  // TODO(#1581): Rename once all uses migrated to coroutines.
  @Update suspend fun updateSuspend(entity: E): Int

  @Update fun updateAll(entities: List<E>): Completable

  @Deprecated("Replace usage with deleteSuspend") @Delete fun delete(entity: E): Completable

  // TODO(#1581): Rename to delete once all existing usages are migrated to coroutine.
  @Delete suspend fun deleteSuspend(entity: E)
}

/** Try to update the specified entity, and if it doesn't yet exist, create it. */
@Deprecated("Use insertOrUpdateSuspend instead")
fun <E> BaseDao<E>.insertOrUpdate(entity: E): Completable =
  update(entity).filter { n: Int -> n == 0 }.flatMapCompletable { insert(entity) }

// TODO(#1581): Rename once all uses migrated to coroutines.
/** Try to update the specified entity, and if it doesn't yet exist, create it. Main-safe. */
suspend fun <E> BaseDao<E>.insertOrUpdateSuspend(entity: E) {
  val count = updateSuspend(entity)
  if (count == 0) {
    insertSuspend(entity)
  }
}
