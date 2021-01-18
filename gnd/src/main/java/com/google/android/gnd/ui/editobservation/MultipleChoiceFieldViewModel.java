/*
 * Copyright 2020 Google LLC
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

package com.google.android.gnd.ui.editobservation;

import android.app.Application;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.model.observation.MultipleChoiceResponse;
import com.google.android.gnd.rx.Nil;
import com.google.android.gnd.rx.annotations.Hot;
import java8.util.Optional;
import javax.inject.Inject;

public class MultipleChoiceFieldViewModel extends AbstractFieldViewModel {

  @Hot(replays = true)
  private final MutableLiveData<Nil> showDialogClicks = new MutableLiveData<>();

  @Inject
  MultipleChoiceFieldViewModel(Application application) {
    super(application);
  }

  public void onShowDialog() {
    showDialogClicks.setValue(Nil.NIL);
  }

  MutableLiveData<Nil> getShowDialogClicks() {
    return showDialogClicks;
  }

  Optional<MultipleChoiceResponse> getCurrentResponse() {
    return getResponse().getValue() == null
        ? Optional.empty()
        : getResponse().getValue().map(response -> (MultipleChoiceResponse) response);
  }
}
