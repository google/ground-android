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

package com.google.android.gnd.ui.editobservation;

import android.content.res.Resources;
import com.google.android.gnd.model.observation.TimeResponse;
import com.google.android.gnd.rx.Nil;
import com.google.android.gnd.rx.annotations.Hot;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.util.Date;
import javax.inject.Inject;

public class TimeFieldViewModel extends AbstractFieldViewModel {

  @Hot private final Subject<Nil> showDialogClicks = PublishSubject.create();

  @Inject
  TimeFieldViewModel(Resources resources) {
    super(resources);
  }

  public void updateResponse(Date date) {
    setResponse(TimeResponse.fromDate(date));
  }

  public void onShowDialogClick() {
    showDialogClicks.onNext(Nil.NIL);
  }

  Observable<Nil> getShowDialogClicks() {
    return showDialogClicks;
  }
}
