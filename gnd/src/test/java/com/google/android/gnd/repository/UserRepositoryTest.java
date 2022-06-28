/*
 * Copyright 2021 Google LLC
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

package com.google.android.gnd.repository;

import static com.google.common.truth.Truth.assertThat;

import com.google.android.gnd.BaseHiltTest;
import com.google.android.gnd.FakeData;
import com.google.android.gnd.model.Role;
import com.google.android.gnd.model.Survey;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.local.LocalValueStore;
import com.google.android.gnd.system.auth.FakeAuthenticationManager;
import com.google.common.collect.ImmutableMap;
import dagger.hilt.android.testing.HiltAndroidTest;
import java.util.NoSuchElementException;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
public class UserRepositoryTest extends BaseHiltTest {

  @Inject FakeAuthenticationManager fakeAuthenticationManager;
  @Inject LocalDataStore localDataStore;
  @Inject LocalValueStore localValueStore;
  @Inject UserRepository userRepository;

  @Test
  public void testGetCurrentUser() {
    fakeAuthenticationManager.setUser(null);
    assertThat(userRepository.getCurrentUser()).isNull();
    fakeAuthenticationManager.setUser(FakeData.USER);
    assertThat(userRepository.getCurrentUser()).isEqualTo(FakeData.USER);
  }

  @Test
  public void testGetUserRole() {
    Survey survey =
        FakeData.SURVEY.toBuilder()
            .setAcl(ImmutableMap.of(FakeData.USER.getEmail(), "data_collector"))
            .build();

    // Current user is authorized as contributor.
    fakeAuthenticationManager.setUser(FakeData.USER);
    assertThat(userRepository.getUserRole(survey)).isEqualTo(Role.DATA_COLLECTOR);

    // Current user is unauthorized.
    fakeAuthenticationManager.setUser(FakeData.USER_2);
    assertThat(userRepository.getUserRole(survey)).isEqualTo(Role.UNKNOWN);
  }

  @Test
  public void testSaveUser() {
    localDataStore
        .getUser(FakeData.USER.getId())
        .test()
        .assertFailure(NoSuchElementException.class);
    userRepository.saveUser(FakeData.USER).test().assertComplete();
    localDataStore.getUser(FakeData.USER.getId()).test().assertResult(FakeData.USER);
  }

  @Test
  public void testClearUserPreferences_returnsEmptyLastActiveSurvey() {
    localValueStore.setLastActiveSurveyId("foo");
    userRepository.clearUserPreferences();
    assertThat(localValueStore.getLastActiveSurveyId()).isEmpty();
  }
}
