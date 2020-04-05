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

package com.google.android.gnd.persistence.local.room.dao;

import androidx.test.runner.AndroidJUnit4;
import com.google.android.gnd.inject.DaggerTestComponent;
import com.google.android.gnd.model.User;
import com.google.android.gnd.persistence.local.room.LocalDatabase;
import com.google.android.gnd.persistence.local.room.entity.UserEntity;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UserDaoTest {

  @Inject LocalDatabase database;

  private UserDao userDao;

  @Before
  public void setUp() {
    DaggerTestComponent.create().inject(this);
    userDao = database.userDao();
  }

  @After
  public void tearDown() {
    database.close();
  }

  private User createTestUser(String id, String displayName, String email) {
    return User.builder().setId(id).setDisplayName(displayName).setEmail(email).build();
  }

  @Test
  public void testInsertUser() {
    User user = createTestUser("id_1", "foo name", "foo@gmail.com");
    userDao.insert(UserEntity.fromUser(user)).test().assertNoErrors();
    userDao.findById(user.getId()).test().assertNoErrors().assertValue(UserEntity.fromUser(user));
  }

  @Test
  public void insertingAlreadyPresentUser_shouldRaiseError() {
    User user = createTestUser("id_1", "foo name", "foo@gmail.com");
    userDao.insert(UserEntity.fromUser(user)).test().assertNoErrors();

    userDao
        .insert(UserEntity.fromUser(user))
        .test()
        .assertError(
            throwable -> throwable.getMessage().startsWith("UNIQUE constraint failed: user.id"));
  }

  @Test
  public void testUpdateUser_ifAlreadyPresent() {
    User user = createTestUser("id_1", "foo name", "foo@gmail.com");
    userDao.insert(UserEntity.fromUser(user)).test().assertNoErrors();
    userDao.insertOrUpdate(UserEntity.fromUser(user)).test().assertNoErrors();
  }

  @Test
  public void testDeleteUser() {
    User user = createTestUser("id_2", "foo name 2", "foo2@gmail.com");
    userDao.insert(UserEntity.fromUser(user)).blockingAwait();
    userDao.findById("id_1").test().assertNoValues();
    userDao.findById(user.getId()).test().assertValue(UserEntity.fromUser(user));

    userDao.delete(UserEntity.fromUser(user)).blockingAwait();
    userDao.findById("id_2").test().assertNoValues();
  }

  @Test
  public void testUpdateUser() {
    User user = createTestUser("id", "foo name", "foo@gmail.com");
    userDao.insert(UserEntity.fromUser(user)).blockingAwait();

    user = createTestUser("id", "new foo name", "newfoo@gmail.com");
    userDao.update(UserEntity.fromUser(user)).test().assertValue(1);
    userDao.findById("id").test().assertValue(UserEntity.fromUser(user));
  }
}
