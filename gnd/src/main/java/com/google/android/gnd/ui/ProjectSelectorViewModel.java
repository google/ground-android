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
package com.google.android.gnd.ui;

import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModel;
import com.google.android.gnd.repository.GndDataRepository;
import javax.inject.Inject;

public class ProjectSelectorViewModel extends ViewModel {
  private final GndDataRepository dataRepository;

  @Inject
  ProjectSelectorViewModel(GndDataRepository dataRepository) {
    this.dataRepository = dataRepository;
  }

  @SuppressLint("CheckResult")
  public void activateProject(String id) {
    dataRepository.activateProject(id);
  }
}
