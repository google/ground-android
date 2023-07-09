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
import com.google.android.ground.rx.annotations.Cold
import io.reactivex.Single

/** Provides access to [User] data in local storage. */
interface LocalUserStore {
  /** Add user to the database. */
  suspend fun insertOrUpdateUser(user: User)

  /**
   * Loads the user with the specified id from the local data store. The returned Single fails with
   * [java.util.NoSuchElementException] if not found.
   */
  @Deprecated("Use getUserSuspend instead") fun getUser(id: String): @Cold Single<User>

  // TODO(#1581): Rename to getUser once all existing usages are migrated to coroutine.
  suspend fun getUserSuspend(id: String): User
}
