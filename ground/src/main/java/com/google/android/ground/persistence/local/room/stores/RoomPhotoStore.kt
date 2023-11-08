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

import com.google.android.ground.model.Photo
import com.google.android.ground.persistence.local.room.converter.toLocalDataStoreObject
import com.google.android.ground.persistence.local.room.converter.toModelObject
import com.google.android.ground.persistence.local.room.dao.PhotoDao
import com.google.android.ground.persistence.local.room.dao.insertOrUpdate
import com.google.android.ground.persistence.local.stores.LocalStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
/** Provides access to [Photo]s saved to the device's local storage. */
class RoomPhotoStore @Inject internal constructor() : LocalStore<Photo> {
  @Inject lateinit var photoDao: PhotoDao

  /** Retrieves a single [Photo] by its unique identifier. */
  override suspend fun getById(id: String): Photo = photoDao.get(id).toModelObject()

  /** Retrieves all [Photo]s currently stored in the local database. */
  override suspend fun getAll(): List<Photo> = photoDao.getAllPhotos().map { it.toModelObject() }

  /** Deletes the [Photo] with the given id from local storage. */
  override suspend fun delete(id: String) {
    val photo = photoDao.get(id)
    photoDao.delete(photo)
  }

  /** Updates (or inserts, if it does not yet exist) the given [Photo] in local storage. */
  override suspend fun insertOrUpdate(model: Photo) =
    photoDao.insertOrUpdate(model.toLocalDataStoreObject())
}
