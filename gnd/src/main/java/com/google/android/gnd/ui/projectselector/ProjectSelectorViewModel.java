/*
 * Copyright 2019 Google LLC
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

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.system.AuthenticationManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.vo.Project;
import io.reactivex.Completable;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

public class ProjectSelectorViewModel extends AbstractViewModel {
  private static final String TAG = ProjectSelectorViewModel.class.getSimpleName();

  private final DataRepository dataRepository;
  private final MutableLiveData<Resource<List<Project>>> projectSummaries;
  private final AuthenticationManager authManager;

  @Inject
  ProjectSelectorViewModel(DataRepository dataRepository, AuthenticationManager authManager) {
    this.dataRepository = dataRepository;
    this.projectSummaries = new MutableLiveData<>();
    this.authManager = authManager;
  }

  // TODO: Show message when no visible projects found.
  public void loadProjectSummaries() {
    // TODO(#24): Fix leaky subscriptions!
    disposeOnClear(
        authManager
            .getUser()
            .flatMap(user -> dataRepository.getProjectSummaries(user))
            .subscribe(summaries -> projectSummaries.setValue(summaries)));
  }

  public LiveData<Resource<List<Project>>> getProjectSummaries() {
    return projectSummaries;
  }

  Completable activateProject(int idx) {
    return dataRepository.activateProject(
        Resource.getData(this.projectSummaries).orElse(Collections.emptyList()).get(idx).getId());
  }
}
