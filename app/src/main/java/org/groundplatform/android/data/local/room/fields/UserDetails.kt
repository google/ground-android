/*
 * Copyright 2020 Google LLC
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
package org.groundplatform.android.data.local.room.fields

import androidx.room.ColumnInfo
import org.groundplatform.android.model.User

/** Component representing cached user details in local db entities. */
data class UserDetails(
  @ColumnInfo(name = "id") val id: String,
  @ColumnInfo(name = "email") val email: String,
  @ColumnInfo(name = "display_name") val displayName: String,
) {
  companion object {
    fun fromUser(user: User) = UserDetails(user.id, user.email, user.displayName)

    fun toUser(d: UserDetails) = User(d.id, d.email, d.displayName)

    @JvmStatic
    fun create(id: String?, email: String?, displayName: String?) =
      UserDetails(id!!, email ?: "", displayName ?: "")
  }
}
