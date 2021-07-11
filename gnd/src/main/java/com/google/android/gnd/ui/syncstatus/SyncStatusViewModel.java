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

package com.google.android.gnd.ui.syncstatus;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.repository.ProjectRepository;
import com.google.android.gnd.rx.annotations.Cold;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.offlinebasemap.OfflineBaseMapsFragmentDirections;
import com.google.common.collect.ImmutableList;
import io.reactivex.Flowable;
import javax.inject.Inject;

/**
 * View model for the offline area manager fragment. Handles the current list of downloaded areas.
 */
public class SyncStatusViewModel extends AbstractViewModel {

  private LiveData<ImmutableList<Mutation>> mutations;
  private final Navigator navigator;

  @Inject
  SyncStatusViewModel(ProjectRepository projectRepository, Navigator navigator) {
    this.navigator = navigator;

    this.mutations =
        LiveDataReactiveStreams.fromPublisher(
            projectRepository
                .getActiveProject()
                .switchMap(
                    project ->
                        project
                            .map(projectRepository::getMutationsOnceAndStream)
                            .orElse(Flowable.just(ImmutableList.of()))));
  }

  public void showOfflineAreaSelector() {
    navigator.navigate(OfflineBaseMapsFragmentDirections.showOfflineAreaSelector());
  }

  @Cold(replays = true)
  LiveData<ImmutableList<Mutation>> getMutations() {
    return mutations;
  }
}
