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

package com.google.android.gnd.dao;

import com.google.android.gnd.TestApplication;
import com.google.android.gnd.inject.DaggerTestComponent;
import com.google.android.gnd.model.User;
import com.google.android.gnd.persistence.local.room.LocalDatabase;
import com.google.android.gnd.persistence.local.room.dao.UserDao;
import com.google.android.gnd.persistence.local.room.entity.UserEntity;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(application = TestApplication.class)
public class UserDaoTest {

  @Inject UserDao userDao;
  @Inject LocalDatabase database;

  private User testUser =
      User.builder().setId("foo id").setDisplayName("foo name").setEmail("foo@gmail.com").build();

  @Before
  public void setUp() {
    DaggerTestComponent.create().inject(this);
  }

  @After
  public void tearDown() {
    database.close();
  }

  @Test
  public void testEmptyDb() {
    userDao.findById(testUser.getId()).test().assertNoValues();
  }

  @Test
  public void testInsertUser() {
    userDao.insert(UserEntity.fromUser(testUser)).test().assertNoErrors();
    userDao
        .findById(testUser.getId())
        .test()
        .assertNoErrors()
        .assertValue(UserEntity.fromUser(testUser));
  }

  @Test
  public void testInsertUser_alreadyPresent_raisesError() {
    userDao.insert(UserEntity.fromUser(testUser)).blockingAwait();
    userDao
        .insert(UserEntity.fromUser(testUser))
        .test()
        .assertError(
            throwable ->
                throwable.getMessage().startsWith("Cannot execute for last inserted row ID"));
  }

  @Test
  public void testInsertOrUpdateUser() {
    userDao.insert(UserEntity.fromUser(testUser)).test().assertNoErrors();
    userDao.insertOrUpdate(UserEntity.fromUser(testUser)).test().assertNoErrors();
  }

  @Test
  public void testDeleteUser() {
    userDao.insert(UserEntity.fromUser(testUser)).blockingAwait();
    userDao.delete(UserEntity.fromUser(testUser)).blockingAwait();
    userDao.findById(testUser.getId()).test().assertNoValues();
  }

  @Test
  public void testUpdateUser() {
    userDao.insert(UserEntity.fromUser(testUser)).blockingAwait();
    User updatedUser =
        User.builder()
            .setId(testUser.getId())
            .setDisplayName("new foo name")
            .setEmail("newfoo@gmail.com")
            .build();
    userDao.update(UserEntity.fromUser(updatedUser)).test().assertValue(1);
    userDao.findById(testUser.getId()).test().assertValue(UserEntity.fromUser(updatedUser));
  }
}
