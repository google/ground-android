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
package com.google.android.ground.persistence.local.stores

import com.google.android.ground.model.User
import com.google.android.ground.persistence.local.room.LocalDataStoreException

/** Provides access to [User] data in local storage. */
interface LocalUserStore {
  /** Add user to the database. */
  suspend fun insertOrUpdateUser(user: User)

  /**
   * Loads the [User] with the specified id from the local data store.
   *
   * @throws LocalDataStoreException if the user is not found.
   */
  suspend fun getUser(id: String): User

  /**
   * Loads the [User] with the specified id from the local data store. If not found, returns null.
   */
  suspend fun getUserOrNull(id: String): User?
}
