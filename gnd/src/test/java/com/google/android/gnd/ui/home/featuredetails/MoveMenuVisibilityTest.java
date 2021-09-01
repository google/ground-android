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

package com.google.android.gnd.ui.home.featuredetails;

import static com.google.common.truth.Truth.assertThat;

import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Feature;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class MoveMenuVisibilityTest extends BaseFeatureDetailsViewModelTest {

  @Parameterized.Parameter() public User user;

  @Parameterized.Parameter(1)
  public Feature feature;

  @Parameterized.Parameter(2)
  public boolean visible;

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    Object[][] data = {

      // Point feature created by some other user
      {TEST_USER_OWNER, createPointFeature(TEST_USER_UNKNOWN), true},
      {TEST_USER_MANAGER, createPointFeature(TEST_USER_UNKNOWN), true},
      {TEST_USER_CONTRIBUTOR, createPointFeature(TEST_USER_UNKNOWN), false},

      // Polygon feature created by some other user
      {TEST_USER_OWNER, createPolygonFeature(TEST_USER_UNKNOWN), false},
      {TEST_USER_MANAGER, createPolygonFeature(TEST_USER_UNKNOWN), false},
      {TEST_USER_CONTRIBUTOR, createPolygonFeature(TEST_USER_UNKNOWN), false},

      // Current user created the selected feature
      {TEST_USER_CONTRIBUTOR, createPointFeature(TEST_USER_CONTRIBUTOR), true},
      {TEST_USER_CONTRIBUTOR, createPolygonFeature(TEST_USER_CONTRIBUTOR), false},
    };
    return Arrays.asList(data);
  }

  @Test
  public void testMoveMenuVisible() {
    mockCurrentUser(user);
    setSelectedFeature(feature);

    TestObservers.observeUntilFirstChange(viewModel.isMoveMenuOptionVisible());
    assertThat(viewModel.isMoveMenuOptionVisible().getValue()).isEqualTo(visible);
  }
}
