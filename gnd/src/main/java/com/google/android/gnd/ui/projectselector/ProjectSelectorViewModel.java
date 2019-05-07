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

import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.rx.Result;
import com.google.android.gnd.system.AuthenticationManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.vo.Project;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class ProjectSelectorViewModel extends AbstractViewModel {
  private static final String TAG = ProjectSelectorViewModel.class.getSimpleName();

  private final DataRepository dataRepository;
  private final MutableLiveData<Resource<List<Project>>> projectSummaries;
  private final PublishSubject<Integer> projectSelections;
  public final MutableLiveData<Project> activeProject;
  public final MutableLiveData<Throwable> activateProjectErrors;
  public final Observable<Result<Project>> activeProjectStream;

  @Inject
  ProjectSelectorViewModel(DataRepository dataRepository, AuthenticationManager authManager) {
    this.dataRepository = dataRepository;
    this.projectSummaries = new MutableLiveData<>();
    this.activeProject = new MutableLiveData<>();
    this.activateProjectErrors = new MutableLiveData<>();
    this.projectSelections = PublishSubject.create();

    AuthenticationManager.User user =
        authManager.getUser().blockingFirst(AuthenticationManager.User.ANONYMOUS);

    Observable<Resource<List<Project>>> availableProjects =
        dataRepository.getProjectSummaries(user);

    this.activeProjectStream =
        projectSelections.switchMap(Result.mapObservable(this::selectActiveProject));

    disposeOnClear(
        activeProjectStream.subscribe(
            Result.unwrap(activeProject::setValue, this::onActiveProjectError)));

    disposeOnClear(
        availableProjects.subscribe(projectSummaries::setValue, this::onProjectSummariesError));
  }

  public LiveData<Resource<List<Project>>> getProjectSummaries() {
    return projectSummaries;
  }

  public LiveData<Project> getActiveProject() {
    return activeProject;
  }

  private Observable<Project> selectActiveProject(int idx) {
    return this.dataRepository
        .activateProject(
            Resource.getData(this.projectSummaries)
                .orElse(Collections.emptyList())
                .get(idx)
                .getId())
        .toObservable();
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
