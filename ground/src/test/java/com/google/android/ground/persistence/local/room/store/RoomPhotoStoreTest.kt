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
package com.google.android.ground.persistence.local.room.store

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.persistence.local.room.stores.RoomLocationOfInterestStore
import com.google.android.ground.persistence.local.room.stores.RoomPhotoStore
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class RoomPhotoStoreTest : BaseHiltTest() {
  @Inject lateinit var roomPhotoStore: RoomPhotoStore
  @Inject lateinit var locationOfInterestStore: RoomLocationOfInterestStore

  // Can't suspend @Before methods....
  private suspend fun insertLOI() {
    // Establish foreign keys in the DB
    locationOfInterestStore.insertOrUpdate(FakeData.LOCATION_OF_INTEREST)
  }

  @Test
  fun insertOrUpdate_insertsAPhoto() = runWithTestDispatcher {
    insertLOI()
    roomPhotoStore.insertOrUpdate(FakeData.PHOTO)
    assertThat(roomPhotoStore.getById(FakeData.PHOTO.id)).isEqualTo(FakeData.PHOTO)
  }

  @Test
  fun delete_deletesAnExistingPhoto() = runWithTestDispatcher {
    insertLOI()
    roomPhotoStore.insertOrUpdate(FakeData.PHOTO)
    roomPhotoStore.delete(FakeData.PHOTO.id)
    assertThat(roomPhotoStore.getAll()).isEmpty()
  }

  @Test
  fun getById_returnsAPhotoAccordingToItsID() = runWithTestDispatcher {
    insertLOI()
    val photo = FakeData.PHOTO.copy(id = "5678")
    roomPhotoStore.insertOrUpdate(FakeData.PHOTO)
    roomPhotoStore.insertOrUpdate(photo)
    assertThat(roomPhotoStore.getById(photo.id)).isEqualTo(photo)
    assertThat(roomPhotoStore.getById(FakeData.PHOTO.id)).isEqualTo(FakeData.PHOTO)
  }

  @Test
  fun getAll_returnsAllPhotosInLocalStorage() = runWithTestDispatcher {
    insertLOI()
    val photos = buildList {
      for (i in 0..10) {
        val photo = FakeData.PHOTO.copy(id = "$i")
        add(photo)
        roomPhotoStore.insertOrUpdate(photo)
      }
    }

    assertThat(roomPhotoStore.getAll()).containsExactlyElementsIn(photos)
  }
}
