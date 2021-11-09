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

import static com.google.android.gnd.TestObservers.observeUntilFirstChange;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.android.gnd.BaseHiltTest;
import com.google.android.gnd.model.observation.TimeResponse;
import com.google.android.gnd.rx.Nil;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import java.util.Date;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
public class TimeFieldViewModelTest extends BaseHiltTest {

  // Date represented in milliseconds for date: 2021-09-24T16:40+0000.
  private static final Date DATE = new Date(1632501600000L);

  @Inject TimeFieldViewModel timeFieldViewModel;

  @Test
  public void testUpdateResponse() {
    timeFieldViewModel.updateResponse(DATE);
    observeUntilFirstChange(timeFieldViewModel.getResponse());
    TimeResponse response = (TimeResponse) timeFieldViewModel.getResponse().getValue().get();
    assertThat(response.getTime()).isEqualTo(new TimeResponse(DATE).getTime());
  }

  @Test
  public void testUpdateResponse_mismatchTime() {
    timeFieldViewModel.updateResponse(DATE);
    observeUntilFirstChange(timeFieldViewModel.getResponse());
    TimeResponse response = (TimeResponse) timeFieldViewModel.getResponse().getValue().get();
    assertThat(response.getTime()).isNotEqualTo(new TimeResponse(new Date()).getTime());
  }

  @Test
  public void testDialogClick() {
    timeFieldViewModel = mock(TimeFieldViewModel.class);
    when(timeFieldViewModel.getShowDialogClicks()).thenReturn(Observable.just(Nil.NIL));
    Observable<Nil> observable = timeFieldViewModel.getShowDialogClicks();
    TestObserver<Nil> observer = new TestObserver<>();
    observable.subscribe(observer);

    observer.assertComplete().assertNoErrors().assertValue(Nil.NIL).assertValueCount(1);
  }
}
