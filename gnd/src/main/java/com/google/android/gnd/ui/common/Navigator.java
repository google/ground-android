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
package com.google.android.gnd.ui.common;

import androidx.navigation.NavDirections;
import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.ui.home.HomeScreenFragmentDirections;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import javax.inject.Inject;

/**
 * Responsible for abstracting navigation from fragment to fragment. Exposes various actions to
 * ViewModels that cause a NavDirections to be emitted to the observer (in this case, the {@link
 * com.google.android.gnd.MainActivity}, which is expected to pass it to the current {@link
 * androidx.navigation.NavController}.
 */
@ActivityScoped
public class Navigator {
  private final Subject<NavDirections> navDirections;

  @Inject
  public Navigator() {
    this.navDirections = PublishSubject.create();
  }

  public Observable<NavDirections> getNavDirections() {
    return navDirections;
  }

  /** Shows the "Add Record" fragment for the specified form. */
  public void addRecord(String projectId, String placeId, String formId) {
    navDirections.onNext(HomeScreenFragmentDirections.addRecord(projectId, placeId, formId));
  }
}
