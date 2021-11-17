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
import com.google.android.gnd.model.Project;
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
public class ProjectRepositoryTest extends BaseHiltTest {

  @BindValue @Mock LocalDataStore mockLocalDataStore;
  @BindValue @Mock UserRepository userRepository;

  @Inject ProjectRepository projectRepository;
  @Inject FakeRemoteDataStore fakeRemoteDataStore;

  @Test
  public void testActivateProject() {
    Project project = newProject().build();
    setTestProject(project);

    projectRepository.activateProject("id");

    projectRepository.getActiveProject().test().assertValue(Optional.of(project));
  }

  @Test
  public void testActivateProject_managersCanAddFeaturesToAllLayers() {
    Layer layer = newLayer().setId("Layer").setContributorsCanAdd(ImmutableList.of()).build();
    setTestProject(newProject().putLayer(layer.getId(), layer).build());
    when(userRepository.getUserRole(any())).thenReturn(Role.MANAGER);

    projectRepository.activateProject("id");

    Layer expectedLayer = layer.toBuilder().setUserCanAdd(FeatureType.ALL).build();
    projectRepository
        .getActiveProject()
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
    setTestProject(
        newProject()
            .putLayer(layer1.getId(), layer1)
            .putLayer(layer2.getId(), layer2)
            .putLayer(layer3.getId(), layer3)
            .putLayer(layer4.getId(), layer4)
            .build());
    when(userRepository.getUserRole(any())).thenReturn(Role.CONTRIBUTOR);

    projectRepository.activateProject("id");

    ImmutableList<Layer> expectedLayers =
        ImmutableList.of(
            layer1.toBuilder().setUserCanAdd(ImmutableList.of(FeatureType.POINT)).build(),
            layer2.toBuilder().setUserCanAdd(ImmutableList.of(FeatureType.POLYGON)).build(),
            layer3.toBuilder().setUserCanAdd(FeatureType.ALL).build(),
            layer4.toBuilder().setUserCanAdd(ImmutableList.of()).build());
    projectRepository
        .getActiveProject()
        .test()
        .assertValue(p -> p.get().getLayers().equals(expectedLayers));
  }

  private void setTestProject(Project project) {
    fakeRemoteDataStore.setTestProject(project);
    when(mockLocalDataStore.getProjectById(any())).thenReturn(Maybe.just(project));
  }
}
