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

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.gnd.MainViewModel;
import com.google.android.gnd.ui.editobservation.DateFieldViewModel;
import com.google.android.gnd.ui.editobservation.EditObservationViewModel;
import com.google.android.gnd.ui.editobservation.MultipleChoiceFieldViewModel;
import com.google.android.gnd.ui.editobservation.NumberFieldViewModel;
import com.google.android.gnd.ui.editobservation.PhotoFieldViewModel;
import com.google.android.gnd.ui.editobservation.TextFieldViewModel;
import com.google.android.gnd.ui.editobservation.TimeFieldViewModel;
import com.google.android.gnd.ui.home.HomeScreenViewModel;
import com.google.android.gnd.ui.home.featuredetails.FeatureDetailsViewModel;
import com.google.android.gnd.ui.home.featuredetails.ObservationListItemViewModel;
import com.google.android.gnd.ui.home.featuredetails.ObservationListViewModel;
import com.google.android.gnd.ui.home.featureselector.FeatureSelectorViewModel;
import com.google.android.gnd.ui.home.mapcontainer.MapContainerViewModel;
import com.google.android.gnd.ui.observationdetails.ObservationDetailsViewModel;
import com.google.android.gnd.ui.offlinebasemap.OfflineBaseMapsViewModel;
import com.google.android.gnd.ui.offlinebasemap.selector.OfflineBaseMapSelectorViewModel;
import com.google.android.gnd.ui.offlinebasemap.viewer.OfflineBaseMapViewerViewModel;
import com.google.android.gnd.ui.projectselector.ProjectSelectorViewModel;
import com.google.android.gnd.ui.signin.SignInViewModel;
import com.google.android.gnd.ui.syncstatus.SyncStatusViewModel;
import com.google.android.gnd.ui.tos.TermsOfServiceViewModel;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ApplicationComponent;
import dagger.multibindings.IntoMap;

@InstallIn(ApplicationComponent.class)
@Module
public abstract class ViewModelModule {
  @Binds
  @IntoMap
  @ViewModelKey(MapContainerViewModel.class)
  abstract ViewModel bindMapContainerViewModel(MapContainerViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(OfflineBaseMapSelectorViewModel.class)
  abstract ViewModel bindOfflineAreaSelectorViewModel(OfflineBaseMapSelectorViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(SyncStatusViewModel.class)
  abstract ViewModel bindSyncStatusViewModel(SyncStatusViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(OfflineBaseMapsViewModel.class)
  abstract ViewModel bindOfflineAreasViewModel(OfflineBaseMapsViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(OfflineBaseMapViewerViewModel.class)
  abstract ViewModel bindOfflineAreaViewerViewModel(OfflineBaseMapViewerViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(MainViewModel.class)
  abstract ViewModel bindMainViewModel(MainViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(SignInViewModel.class)
  abstract ViewModel bindSignInVideModel(SignInViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(TermsOfServiceViewModel.class)
  abstract ViewModel bindTermsViewModel(TermsOfServiceViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(HomeScreenViewModel.class)
  abstract ViewModel bindHomeScreenViewModel(HomeScreenViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(FeatureDetailsViewModel.class)
  abstract ViewModel bindFeatureDetailsViewModel(FeatureDetailsViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(ProjectSelectorViewModel.class)
  abstract ViewModel bindProjectSelectorViewModel(ProjectSelectorViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(ObservationListItemViewModel.class)
  abstract ViewModel bindObservationListItemViewModel(ObservationListItemViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(ObservationListViewModel.class)
  abstract ViewModel bindObservationListViewModel(ObservationListViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(ObservationDetailsViewModel.class)
  abstract ViewModel bindObservationDetailsViewModel(ObservationDetailsViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(EditObservationViewModel.class)
  abstract ViewModel bindEditObservationViewModel(EditObservationViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(PhotoFieldViewModel.class)
  abstract ViewModel bindPhotoFieldViewModel(PhotoFieldViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(MultipleChoiceFieldViewModel.class)
  abstract ViewModel bindMultipleChoiceFieldViewModel(MultipleChoiceFieldViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(TextFieldViewModel.class)
  abstract ViewModel bindTextFieldViewModel(TextFieldViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(NumberFieldViewModel.class)
  abstract ViewModel bindNumberFieldViewModel(NumberFieldViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(DateFieldViewModel.class)
  abstract ViewModel bindDateFieldViewModel(DateFieldViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(TimeFieldViewModel.class)
  abstract ViewModel bindTimeFieldViewModel(TimeFieldViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(FeatureSelectorViewModel.class)
  abstract ViewModel bindFeatureSelectorViewModel(FeatureSelectorViewModel viewModel);

  @Binds
  abstract ViewModelProvider.Factory bindViewModelFactory(ViewModelFactory factory);
}
