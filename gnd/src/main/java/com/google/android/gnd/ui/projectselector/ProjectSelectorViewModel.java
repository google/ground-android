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
import com.google.android.gnd.repository.Persistable;
import com.google.android.gnd.system.AuthenticationManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

public class ProjectSelectorViewModel extends AbstractViewModel {
  private static final String TAG = ProjectSelectorViewModel.class.getSimpleName();

  private final MutableLiveData<Persistable<List<Project>>> projectSummaries;
  private final PublishSubject<Integer> projectSelections;
  private final MutableLiveData<Project> activeProject;
  private final MutableLiveData<Throwable> activateProjectErrors;

  @Inject
  ProjectSelectorViewModel(DataRepository dataRepository, AuthenticationManager authManager) {
    this.projectSummaries = new MutableLiveData<>();
    this.activeProject = new MutableLiveData<>();
    this.activateProjectErrors = new MutableLiveData<>();
    this.projectSelections = PublishSubject.create();

    disposeOnClear(
        projectSelections
            .switchMapSingle(
                idx ->
                    dataRepository
                        .activateProject(getProjectSummary(idx).getId())
                        .doOnError(this::onActiveProjectError)
                        .onErrorResumeNext(Single.never()))
            .subscribe(activeProject::setValue));

    AuthenticationManager.User user =
        authManager.getUser().blockingFirst(AuthenticationManager.User.ANONYMOUS);

    disposeOnClear(
        dataRepository
            .getProjectSummaries(user)
            .subscribe(projectSummaries::postValue, this::onProjectSummariesError));
  }

  public LiveData<Persistable<List<Project>>> getProjectSummaries() {
    return projectSummaries;
  }

  public LiveData<Throwable> getActivateProjectErrors() {
    return activateProjectErrors;
  }

  public LiveData<Project> getActiveProject() {
    return activeProject;
  }

  private Project getProjectSummary(int idx) {
    return Persistable.getData(this.projectSummaries).orElse(Collections.emptyList()).get(idx);
  }

  private void onProjectSummariesError(Throwable t) {
    Log.d(TAG, "Failed to retrieve project summaries.", t);
  }

  private void onActiveProjectError(Throwable t) {
    Log.d(TAG, "Could not activate project.", t);
    this.activateProjectErrors.setValue(t);
  }

  void activateProject(int idx) {
    projectSelections.onNext(idx);
  }
}
