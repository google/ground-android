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

import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.google.android.gnd.FakeData;
import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.PointFeature;
import com.google.android.gnd.model.feature.PolygonFeature;
import com.google.android.gnd.repository.FeatureRepository;
import com.google.android.gnd.repository.ObservationRepository;
import com.google.android.gnd.repository.UserRepository;
import com.google.android.gnd.system.auth.AuthenticationManager;
import com.google.android.gnd.ui.MarkerIconFactory;
import com.google.android.gnd.ui.common.FeatureHelper;
import com.google.android.gnd.ui.util.DrawableUtil;
import com.google.common.collect.ImmutableMap;
import java8.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class FeatureDetailsViewModelTest {

  static final User TEST_USER_OWNER =
      FakeData.TEST_USER.toBuilder().setEmail("user1@gmail.com").build();

  static final User TEST_USER_MANAGER =
      FakeData.TEST_USER.toBuilder().setEmail("user2@gmail.com").build();

  static final User TEST_USER_CONTRIBUTOR =
      FakeData.TEST_USER.toBuilder().setEmail("user3@gmail.com").build();

  static final User TEST_USER_UNKNOWN =
      FakeData.TEST_USER.toBuilder().setEmail("user4@gmail.com").build();

  private static final Project TEST_PROJECT =
      FakeData.TEST_PROJECT.toBuilder()
          .setAcl(
              ImmutableMap.<String, String>builder()
                  .put(TEST_USER_OWNER.getEmail(), "owner")
                  .put(TEST_USER_MANAGER.getEmail(), "manager")
                  .put(TEST_USER_CONTRIBUTOR.getEmail(), "contributor")
                  .build())
          .build();

  @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock MarkerIconFactory mockMarkerIconFactory;
  @Mock DrawableUtil mockDrawableUtil;
  @Mock FeatureHelper mockFeatureHelper;
  @Mock FeatureRepository mockFeatureRepository;
  @Mock ObservationRepository mockObservationRepository;
  @Mock AuthenticationManager mockAuthManager;

  FeatureDetailsViewModel viewModel;

  static PointFeature createPointFeature() {
    return createPointFeature(FakeData.TEST_USER);
  }

  static PointFeature createPointFeature(User user) {
    return FakeData.TEST_POINT_FEATURE.toBuilder()
        .setProject(TEST_PROJECT)
        .setCreated(AuditInfo.now(user))
        .build();
  }

  static PolygonFeature createPolygonFeature() {
    return createPolygonFeature(FakeData.TEST_USER);
  }

  static PolygonFeature createPolygonFeature(User user) {
    return FakeData.TEST_POLYGON_FEATURE.toBuilder()
        .setProject(TEST_PROJECT)
        .setCreated(AuditInfo.now(user))
        .build();
  }

  @Before
  public void setUp() {
    UserRepository userRepository = new UserRepository(mockAuthManager, null, null, null);
    viewModel =
        new FeatureDetailsViewModel(
            mockMarkerIconFactory,
            mockDrawableUtil,
            mockFeatureHelper,
            mockFeatureRepository,
            mockObservationRepository,
            userRepository);
  }

  void mockCurrentUser(User user) {
    when(mockAuthManager.getCurrentUser()).thenReturn(user);
  }

  void setSelectedFeature(Feature feature) {
    viewModel.onFeatureSelected(Optional.of(feature));
  }
}
