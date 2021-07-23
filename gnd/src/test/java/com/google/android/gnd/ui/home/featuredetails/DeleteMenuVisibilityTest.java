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

import static com.google.android.gnd.model.Role.CONTRIBUTOR;
import static com.google.android.gnd.model.Role.MANAGER;
import static com.google.android.gnd.model.Role.OWNER;
import static com.google.android.gnd.model.Role.UNKNOWN;
import static com.google.android.gnd.model.feature.FeatureType.POINT;
import static com.google.android.gnd.model.feature.FeatureType.POLYGON;
import static com.google.common.truth.Truth.assertThat;

import com.google.android.gnd.TestObservers;
import com.google.android.gnd.model.Role;
import com.google.android.gnd.model.feature.FeatureType;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class DeleteMenuVisibilityTest extends FeatureDetailsViewModelTest {

  @Parameterized.Parameter()
  public Role userRole;

  @Parameterized.Parameter(1)
  public FeatureType featureType;

  @Parameterized.Parameter(2)
  public boolean result;

  @Parameterized.Parameters(name = "{index}: Test with role={0}, featureType={1}, result={2}")
  public static Collection<Object[]> data() {
    Object[][] data = {
      {OWNER, POINT, true},
      {MANAGER, POINT, true},
      {CONTRIBUTOR, POINT, false},
      {UNKNOWN, POINT, false},
      {OWNER, POLYGON, true},
      {MANAGER, POLYGON, true},
      {CONTRIBUTOR, POLYGON, false},
      {UNKNOWN, POLYGON, false},
    };
    return Arrays.asList(data);
  }

  @Test
  public void testDeleteMenuVisible() {
    mockCurrentUserRole(userRole);
    setSelectedFeature(featureType);

    TestObservers.observeUntilFirstChange(viewModel.isDeleteMenuOptionVisible());
    assertThat(viewModel.isDeleteMenuOptionVisible().getValue()).isEqualTo(result);
  }
}
