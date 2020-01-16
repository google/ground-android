/*
 * Copyright 2018 Google LLC
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

package com.google.android.ground;

import android.view.View;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.ground.model.Project;
import com.google.android.ground.repository.FeatureRepository;
import com.google.android.ground.repository.Loadable;
import com.google.android.ground.repository.ProjectRepository;
import com.google.android.ground.ui.common.AbstractViewModel;
import com.google.android.ground.ui.common.Navigator;
import com.google.android.ground.ui.common.SharedViewModel;
import io.reactivex.Completable;
import javax.inject.Inject;

/** Top-level view model representing state of the {@link MainActivity} shared by all fragments. */
@SharedViewModel
public class MainViewModel extends AbstractViewModel {

  private final ProjectRepository projectRepository;
  private final FeatureRepository featureRepository;
  private final Navigator navigator;
  private MutableLiveData<WindowInsetsCompat> windowInsetsLiveData;

  @Inject
  public MainViewModel(
      ProjectRepository projectRepository,
      FeatureRepository featureRepository,
      Navigator navigator) {
    windowInsetsLiveData = new MutableLiveData<>();
    this.projectRepository = projectRepository;
    this.featureRepository = featureRepository;
    this.navigator = navigator;

    // TODO: Move to background service.
    disposeOnClear(
        projectRepository
            .getActiveProjectOnceAndStream()
            .switchMapCompletable(this::syncFeatures)
            .subscribe());
  }

  /**
   * Keeps local features in sync with remote when a project is active, does nothing when no project
   * is active. The stream never completes; syncing stops when subscriptions are disposed of.
   *
   * @param projectLoadable the load state of the currently active project.
   */
  private Completable syncFeatures(Loadable<Project> projectLoadable) {
    return projectLoadable.value().map(featureRepository::syncFeatures).orElse(Completable.never());
  }

  public LiveData<WindowInsetsCompat> getWindowInsets() {
    return windowInsetsLiveData;
  }

  WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat insets) {
    windowInsetsLiveData.setValue(insets);
    return insets;
  }

  public void onSignedOut(int currentNavDestinationId) {
    projectRepository.clearActiveProject();
    navigator.showSignInScreen(currentNavDestinationId);
  }

  public void onSignedIn(int currentNavDestinationId) {
    navigator.showHomeScreen(currentNavDestinationId);
  }
}
