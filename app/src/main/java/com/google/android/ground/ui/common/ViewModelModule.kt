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
package com.google.android.ground.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.ground.MainViewModel
import com.google.android.ground.ui.datacollection.tasks.date.DateTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.location.CaptureLocationTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.multiplechoice.MultipleChoiceTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.number.NumberTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.photo.PhotoTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.point.DropPinTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.polygon.DrawAreaTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.text.TextTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.time.TimeTaskViewModel
import com.google.android.ground.ui.home.HomeScreenViewModel
import com.google.android.ground.ui.home.mapcontainer.HomeScreenMapContainerViewModel
import com.google.android.ground.ui.home.mapcontainer.MapTypeViewModel
import com.google.android.ground.ui.offlineareas.OfflineAreasViewModel
import com.google.android.ground.ui.offlineareas.selector.OfflineAreaSelectorViewModel
import com.google.android.ground.ui.offlineareas.viewer.OfflineAreaViewerViewModel
import com.google.android.ground.ui.signin.SignInViewModel
import com.google.android.ground.ui.startup.StartupViewModel
import com.google.android.ground.ui.surveyselector.SurveySelectorViewModel
import com.google.android.ground.ui.syncstatus.SyncStatusViewModel
import com.google.android.ground.ui.tos.TermsOfServiceViewModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@InstallIn(SingletonComponent::class)
@Module
abstract class ViewModelModule {
  @Binds
  @IntoMap
  @ViewModelKey(DrawAreaTaskViewModel::class)
  abstract fun bindDrawAreaTaskViewModel(viewModel: DrawAreaTaskViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(HomeScreenMapContainerViewModel::class)
  abstract fun bindMapContainerViewModel(viewModel: HomeScreenMapContainerViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(OfflineAreaSelectorViewModel::class)
  abstract fun bindOfflineAreaSelectorViewModel(viewModel: OfflineAreaSelectorViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(SyncStatusViewModel::class)
  abstract fun bindSyncStatusViewModel(viewModel: SyncStatusViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(OfflineAreasViewModel::class)
  abstract fun bindOfflineAreasViewModel(viewModel: OfflineAreasViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(OfflineAreaViewerViewModel::class)
  abstract fun bindOfflineAreaViewerViewModel(viewModel: OfflineAreaViewerViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(MainViewModel::class)
  abstract fun bindMainViewModel(viewModel: MainViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(SignInViewModel::class)
  abstract fun bindSignInVideModel(viewModel: SignInViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(TermsOfServiceViewModel::class)
  abstract fun bindTermsViewModel(viewModel: TermsOfServiceViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(HomeScreenViewModel::class)
  abstract fun bindHomeScreenViewModel(viewModel: HomeScreenViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(SurveySelectorViewModel::class)
  abstract fun bindSurveySelectorViewModel(viewModel: SurveySelectorViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(PhotoTaskViewModel::class)
  abstract fun bindPhotoTaskViewModel(viewModel: PhotoTaskViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(MultipleChoiceTaskViewModel::class)
  abstract fun bindMultipleChoiceTaskViewModel(viewModel: MultipleChoiceTaskViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(TextTaskViewModel::class)
  abstract fun bindTextTaskViewModel(viewModel: TextTaskViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(NumberTaskViewModel::class)
  abstract fun bindNumberTaskViewModel(viewModel: NumberTaskViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(DateTaskViewModel::class)
  abstract fun bindDateTaskViewModel(viewModel: DateTaskViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(TimeTaskViewModel::class)
  abstract fun bindTimeTaskViewModel(viewModel: TimeTaskViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(DropPinTaskViewModel::class)
  abstract fun bindDropPinTaskViewModel(viewModel: DropPinTaskViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(CaptureLocationTaskViewModel::class)
  abstract fun bindCaptureLocationTaskViewModel(viewModel: CaptureLocationTaskViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(BaseMapViewModel::class)
  abstract fun bindBaseMapViewModel(viewModel: BaseMapViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(MapTypeViewModel::class)
  abstract fun bindMapTypeViewModel(viewModel: MapTypeViewModel): ViewModel

  @Binds
  @IntoMap
  @ViewModelKey(StartupViewModel::class)
  abstract fun bindStartupViewModel(viewModel: StartupViewModel): ViewModel

  @Binds abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}
