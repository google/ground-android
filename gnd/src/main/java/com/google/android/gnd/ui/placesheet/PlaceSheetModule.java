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

package com.google.android.gnd.ui.placesheet;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import com.google.android.gnd.inject.PerFragment;
import com.google.android.gnd.ui.common.GndFragmentModule;
import dagger.Module;
import dagger.Provides;

// TODO: Remove "Fragment" from other other Module names.
@Module(includes = GndFragmentModule.class)
public class PlaceSheetModule {

  @Provides
  @PerFragment
  public Fragment fragment(PlaceSheetBodyFragment fragment) {
    return fragment;
  }

  @Provides
  @PerFragment
  public FragmentManager fragmentManager(PlaceSheetBodyFragment fragment) {
    return fragment.getChildFragmentManager();
  }

}


