/*
 * Copyright 2022 Google LLC
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

package com.google.android.gnd.ui.projectselector;


import static android.os.Looper.getMainLooper;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.view.View;
import android.widget.ListView;
import com.google.android.gnd.BaseHiltTest;
import com.google.android.gnd.FakeData;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.local.LocalDataStoreModule;
import com.google.android.gnd.persistence.remote.FakeRemoteDataStore;
import com.google.android.gnd.repository.ProjectRepository;
import com.google.common.collect.ImmutableList;
import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.UninstallModules;
import io.reactivex.Maybe;
import java.util.List;
import java8.util.Optional;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
@UninstallModules({LocalDataStoreModule.class})
public class ProjectSelectorDialogFragmentTest extends BaseHiltTest {

  @Inject ProjectSelectorViewModel viewModel;
  @Inject ProjectRepository projectRepository;
  @Inject FakeRemoteDataStore fakeRemoteDataStore;
  @BindValue @Mock LocalDataStore mockLocalDataStore;

  private ProjectSelectorDialogFragment projectSelectorDialogFragment;

  private Project project1 = FakeData.newProject().setId("1").build();
  private Project project2 = FakeData.newProject().setId("2").build();

  private List<Project> projects = ImmutableList.of(project1, project2);

  @Before
  public void setup() {
    fakeRemoteDataStore.setTestProjects(projects);
//    for (Project project : projects) {
//      localDataStore.insertOrUpdateProject(project);
//    }
    setupFragment();
  }

  private void setupFragment() {
    ActivityController<MainActivity> activityController =
        Robolectric.buildActivity(MainActivity.class);
    MainActivity activity = activityController.setup().get();

    projectSelectorDialogFragment = new ProjectSelectorDialogFragment();

    projectSelectorDialogFragment.showNow(activity.getSupportFragmentManager(),
        ProjectSelectorDialogFragment.class.getSimpleName());
    shadowOf(getMainLooper()).idle();
  }

  @Test
  public void show_projectDialogIsShown() {
    View listView = projectSelectorDialogFragment.getDialog().getCurrentFocus();

    assertThat(listView).isNotNull();
    assertThat(listView.getVisibility()).isEqualTo(View.VISIBLE);
    assertThat(listView.findViewById(R.id.project_name).getVisibility()).isEqualTo(View.VISIBLE);
  }

  @Test
  public void show_projectSelected_projectIsActivated() {
    ListView listView = (ListView) projectSelectorDialogFragment.getDialog().getCurrentFocus();

    when(mockLocalDataStore.getProjectById(eq(project2.getId()))).thenReturn(Maybe.just(project2));
    shadowOf(listView).performItemClick(1);
    shadowOf(getMainLooper()).idle();

    // Verify Dialog is dismissed
    assertThat(projectSelectorDialogFragment.getDialog()).isNull();
    projectRepository.getActiveProject().test().assertValue(Optional.of(project2));
  }
}
