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
package com.google.android.ground.persistence.local.room.stores

import com.google.android.ground.model.User
import com.google.android.ground.persistence.local.room.LocalDataStoreException
import com.google.android.ground.persistence.local.room.converter.toLocalDataStoreObject
import com.google.android.ground.persistence.local.room.converter.toModelObject
import com.google.android.ground.persistence.local.room.dao.UserDao
import com.google.android.ground.persistence.local.room.dao.insertOrUpdate
import com.google.android.ground.persistence.local.stores.LocalUserStore
import com.google.android.ground.rx.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

/** Manages access to [User] objects persisted in local storage. */
@Singleton
class RoomUserStore @Inject internal constructor() : LocalUserStore {
  @Inject lateinit var userDao: UserDao
  @Inject lateinit var schedulers: Schedulers

  /**
   * Attempts to update persisted data associated with a [User] in the local database. If the
   * provided user does not exist, inserts the given user into the database.
   */
  override suspend fun insertOrUpdateUser(user: User) =
    userDao.insertOrUpdate(user.toLocalDataStoreObject())

  /** Attempts to retrieve the [User] with the given ID from the local database. */
  override suspend fun getUser(id: String): User =
    getUserOrNull(id) ?: throw LocalDataStoreException("Error loading user from local db: $id")

  override suspend fun getUserOrNull(id: String): User? = userDao.findById(id)?.toModelObject()
}
