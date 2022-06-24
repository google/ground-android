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
import com.google.android.gnd.ui.datacollection.DataCollectionViewModel;
import com.google.android.gnd.ui.editsubmission.DateFieldViewModel;
import com.google.android.gnd.ui.editsubmission.EditSubmissionViewModel;
import com.google.android.gnd.ui.editsubmission.MultipleChoiceFieldViewModel;
import com.google.android.gnd.ui.editsubmission.NumberFieldViewModel;
import com.google.android.gnd.ui.editsubmission.PhotoFieldViewModel;
import com.google.android.gnd.ui.editsubmission.TextFieldViewModel;
import com.google.android.gnd.ui.editsubmission.TimeFieldViewModel;
import com.google.android.gnd.ui.home.HomeScreenViewModel;
import com.google.android.gnd.ui.home.featuredetails.FeatureDetailsViewModel;
import com.google.android.gnd.ui.home.featuredetails.SubmissionListItemViewModel;
import com.google.android.gnd.ui.home.featuredetails.SubmissionListViewModel;
import com.google.android.gnd.ui.home.featureselector.FeatureSelectorViewModel;
import com.google.android.gnd.ui.home.mapcontainer.FeatureRepositionViewModel;
import com.google.android.gnd.ui.home.mapcontainer.MapContainerViewModel;
import com.google.android.gnd.ui.home.mapcontainer.PolygonDrawingViewModel;
import com.google.android.gnd.ui.offlinebasemap.OfflineAreasViewModel;
import com.google.android.gnd.ui.offlinebasemap.selector.OfflineAreaSelectorViewModel;
import com.google.android.gnd.ui.offlinebasemap.viewer.OfflineAreaViewerViewModel;
import com.google.android.gnd.ui.signin.SignInViewModel;
import com.google.android.gnd.ui.submissiondetails.SubmissionDetailsViewModel;
import com.google.android.gnd.ui.surveyselector.SurveySelectorViewModel;
import com.google.android.gnd.ui.syncstatus.SyncStatusViewModel;
import com.google.android.gnd.ui.tos.TermsOfServiceViewModel;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.IntoMap;

@InstallIn(SingletonComponent.class)
@Module
public abstract class ViewModelModule {

  @Binds
  @IntoMap
  @ViewModelKey(FeatureRepositionViewModel.class)
  abstract ViewModel bindFeatureRepositionViewModel(FeatureRepositionViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(PolygonDrawingViewModel.class)
  abstract ViewModel bindPolygonDrawingViewModel(PolygonDrawingViewModel viewModel);


  @Binds
  @IntoMap
  @ViewModelKey(MapContainerViewModel.class)
  abstract ViewModel bindMapContainerViewModel(MapContainerViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(OfflineAreaSelectorViewModel.class)
  abstract ViewModel bindOfflineAreaSelectorViewModel(OfflineAreaSelectorViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(SyncStatusViewModel.class)
  abstract ViewModel bindSyncStatusViewModel(SyncStatusViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(DataCollectionViewModel.class)
  abstract ViewModel bindDataCollectionViewModel(DataCollectionViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(OfflineAreasViewModel.class)
  abstract ViewModel bindOfflineAreasViewModel(OfflineAreasViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(OfflineAreaViewerViewModel.class)
  abstract ViewModel bindOfflineAreaViewerViewModel(OfflineAreaViewerViewModel viewModel);

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
  @ViewModelKey(SurveySelectorViewModel.class)
  abstract ViewModel bindSurveySelectorViewModel(SurveySelectorViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(SubmissionListItemViewModel.class)
  abstract ViewModel bindSubmissionListItemViewModel(SubmissionListItemViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(SubmissionListViewModel.class)
  abstract ViewModel bindSubmissionListViewModel(SubmissionListViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(SubmissionDetailsViewModel.class)
  abstract ViewModel bindSubmissionDetailsViewModel(SubmissionDetailsViewModel viewModel);

  @Binds
  @IntoMap
  @ViewModelKey(EditSubmissionViewModel.class)
  abstract ViewModel bindEditSubmissionViewModel(EditSubmissionViewModel viewModel);

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
