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

package com.google.android.ground.ui.home.mapcontainer;

import com.google.android.ground.model.geometry.Point;
import com.google.android.ground.rx.Nil;
import com.google.android.ground.rx.annotations.Hot;
import com.google.android.ground.ui.common.AbstractViewModel;
import com.google.android.ground.ui.common.SharedViewModel;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import javax.annotation.Nullable;
import javax.inject.Inject;

@SharedViewModel
public class LocationOfInterestRepositionViewModel extends AbstractViewModel {

  @Hot private final Subject<Point> confirmButtonClicks = PublishSubject.create();
  @Hot private final Subject<Nil> cancelButtonClicks = PublishSubject.create();
  @Nullable private Point cameraTarget;

  @Inject
  LocationOfInterestRepositionViewModel() {}

  // TODO: Disable the confirm button until the map has not been moved
  public void onConfirmButtonClick() {
    if (cameraTarget != null) {
      confirmButtonClicks.onNext(cameraTarget);
    }
  }

  public void onCancelButtonClick() {
    cancelButtonClicks.onNext(Nil.NIL);
  }

  @Hot
  public Observable<Point> getConfirmButtonClicks() {
    return confirmButtonClicks;
  }

  @Hot
  public Observable<Nil> getCancelButtonClicks() {
    return cancelButtonClicks;
  }

  public void onCameraMoved(Point newTarget) {
    cameraTarget = newTarget;
  }
}
