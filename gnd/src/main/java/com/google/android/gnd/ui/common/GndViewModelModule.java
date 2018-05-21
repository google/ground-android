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

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import com.google.android.gnd.ui.MainViewModel;
import com.google.android.gnd.ui.ProjectSelectorViewModel;
import com.google.android.gnd.ui.mapcontainer.MapContainerViewModel;
import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
public abstract class GndViewModelModule {
  @Binds
  @IntoMap
  @GndViewModelKey(MapContainerViewModel.class)
  abstract ViewModel bindMapContainerViewModel(MapContainerViewModel viewModel);

  @Binds
  @IntoMap
  @GndViewModelKey(MainViewModel.class)
  abstract ViewModel bindMainViewModel(MainViewModel viewModel);

  @Binds
  @IntoMap
  @GndViewModelKey(ProjectSelectorViewModel.class)
  abstract ViewModel bindProjectSelectorViewModel(
    ProjectSelectorViewModel viewModel);

  @Binds
  abstract ViewModelProvider.Factory bindViewModelFactory(GndViewModelFactory factory);
}
