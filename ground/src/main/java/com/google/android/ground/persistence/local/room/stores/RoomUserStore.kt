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
import com.google.android.ground.persistence.local.room.converter.toLocalDataStoreObject
import com.google.android.ground.persistence.local.room.converter.toModelObject
import com.google.android.ground.persistence.local.room.dao.UserDao
import com.google.android.ground.persistence.local.room.dao.insertOrUpdate
import com.google.android.ground.persistence.local.room.dao.insertOrUpdateSuspend
import com.google.android.ground.persistence.local.stores.LocalUserStore
import com.google.android.ground.rx.Schedulers
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/** Manages access to [User] objects persisted in local storage. */
@Singleton
class RoomUserStore @Inject internal constructor() : LocalUserStore {
  @Inject lateinit var userDao: UserDao
  @Inject lateinit var schedulers: Schedulers

  /**
   * Attempts to update persisted data associated with a [User] in the local database. If the
   * provided user does not exist, inserts the given user into the database.
   */
  override fun insertOrUpdateUser(user: User): Completable =
    userDao.insertOrUpdate(user.toLocalDataStoreObject()).subscribeOn(schedulers.io())

  override suspend fun insertOrUpdateUserSuspend(user: User) =
    userDao.insertOrUpdateSuspend(user.toLocalDataStoreObject())

  /**
   * Attempts to retrieve the [User] with the given ID from the local database. If the retrieval
   * fails, returns a [NoSuchElementException].
   */
  override fun getUser(id: String): Single<User> =
    userDao
      .findById(id)
      .doOnError {
        Timber.e(it, "Error loading user from local db: $id")
      } // Fail with NoSuchElementException if not found.
      .toSingle()
      .map { it.toModelObject() }
      .subscribeOn(schedulers.io())
}
