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

package com.google.android.gnd.ui.terms;

import com.google.android.gnd.persistence.local.LocalValueStore;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.Navigator;
import javax.inject.Inject;

public class TermsViewModel extends AbstractViewModel {

  private final Navigator navigator;
  private final LocalValueStore localValueStore;

  @Inject
  public TermsViewModel(Navigator navigator, LocalValueStore localValueStore) {
    this.navigator = navigator;
    this.localValueStore = localValueStore;
  }

  public void onTermsAccepted() {
    localValueStore.termsAccepted();
    navigator.navigate(TermsFragmentDirections.proceedDirectlyToHomeScreen());
  }

}
