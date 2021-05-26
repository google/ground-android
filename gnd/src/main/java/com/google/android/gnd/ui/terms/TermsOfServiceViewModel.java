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

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.model.Terms;
import com.google.android.gnd.persistence.local.LocalValueStore;
import com.google.android.gnd.repository.TermsRepository;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.Navigator;
import javax.inject.Inject;

public class TermsOfServiceViewModel extends AbstractViewModel {

  private final Navigator navigator;

  @Hot(replays = true)
  public final MutableLiveData<String> termsText = new MutableLiveData<>();

  @Hot(replays = true)
  public final MutableLiveData<Boolean> termsCheckBox = new MutableLiveData<>();
  private final LocalValueStore localValueStore;

  private final LiveData<Loadable<Terms>> projectTerms;

  @Inject
  public TermsOfServiceViewModel(Navigator navigator,
      LocalValueStore localValueStore, TermsRepository termsRepository) {
    this.navigator = navigator;
    this.localValueStore = localValueStore;
    this.projectTerms = LiveDataReactiveStreams.fromPublisher(
        termsRepository.getProjectTerms());
  }

  public void onButtonClicked() {
    localValueStore.setTermsAccepted(true);
    navigator.navigate(TermsOfServiceFragmentDirections.proceedDirectlyToHomeScreen());
  }

  public LiveData<Loadable<Terms>> getTerms() {
    return projectTerms;
  }

  public void setTermsTextView(String terms) {
    termsText.setValue(terms);
  }

}
