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

package com.google.android.gnd.ui.projectselector;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.repository.Loadable;
import com.google.android.gnd.system.AuthenticationManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

public class ProjectSelectorViewModel extends AbstractViewModel {
  private static final String TAG = ProjectSelectorViewModel.class.getSimpleName();

  private final DataRepository dataRepository;
  private final MutableLiveData<Loadable<List<Project>>> projectSummaries;

  @Inject
  ProjectSelectorViewModel(DataRepository dataRepository, AuthenticationManager authManager) {
    this.dataRepository = dataRepository;
    this.projectSummaries = new MutableLiveData<>();

    // TODO: Handle activeProject and load error state into HomeScreenViewModel.

    AuthenticationManager.User user =
        authManager.getUser().blockingFirst(AuthenticationManager.User.ANONYMOUS);

    // TODO: Transform project summary stream into LiveData instead of subscribing.
    disposeOnClear(
        dataRepository
            .getProjectSummaries(user)
            .subscribe(projectSummaries::postValue, this::onProjectSummariesError));
  }

  public LiveData<Loadable<List<Project>>> getProjectSummaries() {
    return projectSummaries;
  }

  private Project getProjectSummary(int idx) {
    return Loadable.getData(this.projectSummaries).orElse(Collections.emptyList()).get(idx);
  }

  private void onProjectSummariesError(Throwable t) {
    Log.d(TAG, "Failed to retrieve project summaries.", t);
  }

  void activateProject(int idx) {
    dataRepository.activateProject(getProjectSummary(idx).getId());
  }
}
