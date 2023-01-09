/*
 * Copyright 2023 Google LLC
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

import androidx.room.*

/** Temporary shim to support coroutine migrations. */
@Dao
interface CoroutineDao<E> {
  /** Inserts an entity into the database. */
  @Insert suspend fun insert(entity: E)
  /** Update an entity in the database. Returns the number of updated rows. */
  @Update suspend fun update(entity: E): Int
  /**
   * Updates all database entities in the given list. Returns the number of successfully updated
   * rows.
   */
  @Update suspend fun updateAll(entities: List<E>): Int
  /** Deletes an entity from the database. */
  @Delete suspend fun delete(entity: E)
}

/** Try to update the specified entity, and if it doesn't yet exist, create it. */
@Transaction
suspend fun <E> CoroutineDao<E>.insertOrUpdate(entity: E) {
  if (update(entity) == 0) {
    insert(entity)
  }
}
