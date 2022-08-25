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

package com.google.android.ground.ui.home.locationofinterestdetails;

import static com.google.common.truth.Truth.assertThat;

import com.google.android.ground.model.User;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import com.sharedtest.TestObservers;
import com.sharedtest.persistence.local.LocalDataStoreHelper;
import dagger.hilt.android.testing.HiltAndroidTest;
import java.util.Arrays;
import java.util.Collection;
import java8.util.Optional;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters;

@HiltAndroidTest
@RunWith(ParameterizedRobolectricTestRunner.class)
public class MoveMenuVisibilityTest extends BaseMenuVisibilityTest {

  @Inject LocalDataStoreHelper localDataStoreHelper;

  public MoveMenuVisibilityTest(User user, LocationOfInterest locationOfInterest, boolean visible) {
    super(user, locationOfInterest, visible);
  }

  @Parameters
  public static Collection<Object[]> data() {
    Object[][] data = {

      // Point locationOfInterest created by some other user
      {TEST_USER_OWNER, createPointOfInterest(TEST_USER_UNKNOWN), true},
      {TEST_USER_MANAGER, createPointOfInterest(TEST_USER_UNKNOWN), true},
      {TEST_USER_CONTRIBUTOR, createPointOfInterest(TEST_USER_UNKNOWN), false},

      // Polygon locationOfInterest created by some other user
      {TEST_USER_OWNER, createAreaOfInterest(TEST_USER_UNKNOWN), false},
      {TEST_USER_MANAGER, createAreaOfInterest(TEST_USER_UNKNOWN), false},
      {TEST_USER_CONTRIBUTOR, createAreaOfInterest(TEST_USER_UNKNOWN), false},

      // Current user created the selected locationOfInterest
      {TEST_USER_CONTRIBUTOR, createPointOfInterest(TEST_USER_CONTRIBUTOR), true},
      {TEST_USER_CONTRIBUTOR, createAreaOfInterest(TEST_USER_CONTRIBUTOR), false},
    };
    return Arrays.asList(data);
  }

  @Test
  public void testMoveMenuVisible() {
    fakeAuthenticationManager.setUser(user);
    localDataStoreHelper.insertSurvey(TEST_SURVEY);
    viewModel.onLocationOfInterestSelected(Optional.of(locationOfInterest));

    TestObservers.observeUntilFirstChange(viewModel.isMoveMenuOptionVisible());
    assertThat(viewModel.isMoveMenuOptionVisible().getValue()).isEqualTo(visible);
  }
}
