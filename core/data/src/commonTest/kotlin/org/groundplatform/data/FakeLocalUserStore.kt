/*
 * Copyright 2026 Google LLC
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

package org.groundplatform.data

import org.groundplatform.data.stores.LocalUserStore
import org.groundplatform.domain.model.User

class FakeLocalUserStore : LocalUserStore {
  private val users = mutableMapOf<String, User>()

  override suspend fun insertOrUpdateUser(user: User) {
    users[user.id] = user
  }

  override suspend fun getUser(id: String): User = users[id] ?: error("User not found: $id")

  override suspend fun getUserOrNull(id: String): User? = users[id]
}
