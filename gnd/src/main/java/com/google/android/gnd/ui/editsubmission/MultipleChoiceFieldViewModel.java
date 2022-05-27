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

package com.google.android.gnd.ui.editsubmission;

import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static java8.util.Objects.requireNonNull;
import static java8.util.stream.StreamSupport.stream;

import android.content.res.Resources;
import com.google.android.gnd.model.submission.MultipleChoiceResponse;
import com.google.android.gnd.model.task.Option;
import com.google.android.gnd.rx.Nil;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.common.collect.ImmutableList;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java8.util.Optional;
import javax.inject.Inject;

public class MultipleChoiceFieldViewModel extends AbstractFieldViewModel {

  @Hot
  private final Subject<Nil> showDialogClicks = PublishSubject.create();

  @Inject
  MultipleChoiceFieldViewModel(Resources resources) {
    super(resources);
  }

  public void onShowDialog() {
    showDialogClicks.onNext(Nil.NIL);
  }

  @Hot
  Observable<Nil> getShowDialogClicks() {
    return showDialogClicks;
  }

  Optional<MultipleChoiceResponse> getCurrentResponse() {
    return getResponse().getValue() == null
        ? Optional.empty()
        : getResponse().getValue().map(response -> (MultipleChoiceResponse) response);
  }

  public void updateResponse(ImmutableList<Option> options) {
    setResponse(
        MultipleChoiceResponse.fromList(
            requireNonNull(getField().getMultipleChoice()),
            stream(options).map(Option::getId).collect(toImmutableList())));
  }
}
