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

import static com.google.android.gnd.FakeData.newLayer;
import static com.google.android.gnd.FakeData.newProject;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.android.gnd.BaseHiltTest;
import com.google.android.gnd.model.Survey;
import com.google.android.gnd.model.Role;
import com.google.android.gnd.model.feature.FeatureType;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.local.LocalDataStoreModule;
import com.google.android.gnd.persistence.remote.FakeRemoteDataStore;
import com.google.common.collect.ImmutableList;
import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.UninstallModules;
import io.reactivex.Maybe;
import java8.util.Optional;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

@HiltAndroidTest
@UninstallModules({LocalDataStoreModule.class})
@RunWith(RobolectricTestRunner.class)
public class SurveyRepositoryTest extends BaseHiltTest {

  @BindValue @Mock LocalDataStore mockLocalDataStore;
  @BindValue @Mock UserRepository userRepository;

  @Inject
  SurveyRepository surveyRepository;
  @Inject FakeRemoteDataStore fakeRemoteDataStore;

  @Test
  public void testActivateProject() {
    Survey survey = newProject().build();
    setTestProject(survey);

    surveyRepository.activateSurvey("id");

    surveyRepository.getActiveSurvey().test().assertValue(Optional.of(survey));
  }

  @Test
  public void testActivateProject_managersCanAddFeaturesToAllLayers() {
    Layer layer = newLayer().setId("Layer").setContributorsCanAdd(ImmutableList.of()).build();
    setTestProject(newProject().putLayer(layer).build());
    when(userRepository.getUserRole(any())).thenReturn(Role.MANAGER);

    surveyRepository.activateSurvey("id");

    Layer expectedLayer = layer.toBuilder().setUserCanAdd(FeatureType.ALL).build();
    surveyRepository
        .getActiveSurvey()
        .test()
        .assertValue(p -> p.get().getLayers().equals(ImmutableList.of(expectedLayer)));
  }

  @Test
  public void testActivateProject_contributorsCanAddFeaturesIfAllowed() {
    Layer layer1 =
        newLayer()
            .setId("Layer 1")
            .setContributorsCanAdd(ImmutableList.of(FeatureType.POINT))
            .build();
    Layer layer2 =
        newLayer()
            .setId("Layer 2")
            .setContributorsCanAdd(ImmutableList.of(FeatureType.POLYGON))
            .build();
    Layer layer3 = newLayer().setId("Layer 3").setContributorsCanAdd(FeatureType.ALL).build();
    Layer layer4 = newLayer().setId("Layer 4").setContributorsCanAdd(ImmutableList.of()).build();
    setTestProject(newProject().putLayers(layer1, layer2, layer3, layer4).build());
    when(userRepository.getUserRole(any())).thenReturn(Role.CONTRIBUTOR);

    surveyRepository.activateSurvey("id");

    ImmutableList<Layer> expectedLayers =
        ImmutableList.of(
            layer1.toBuilder().setUserCanAdd(ImmutableList.of(FeatureType.POINT)).build(),
            layer2.toBuilder().setUserCanAdd(ImmutableList.of(FeatureType.POLYGON)).build(),
            layer3.toBuilder().setUserCanAdd(FeatureType.ALL).build(),
            layer4.toBuilder().setUserCanAdd(ImmutableList.of()).build());
    surveyRepository
        .getActiveSurvey()
        .test()
        .assertValue(p -> p.get().getLayers().equals(expectedLayers));
  }

  private void setTestProject(Survey survey) {
    fakeRemoteDataStore.setTestProject(survey);
    when(mockLocalDataStore.getSurveyById(any())).thenReturn(Maybe.just(survey));
  }
}
