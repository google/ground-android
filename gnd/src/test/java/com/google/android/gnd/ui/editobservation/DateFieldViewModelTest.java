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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.google.android.gnd.HiltTestWithRobolectricRunner;
import com.google.android.gnd.model.observation.DateResponse;
import com.google.android.gnd.rx.Nil;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;

@HiltAndroidTest
public class DateFieldViewModelTest extends HiltTestWithRobolectricRunner {

  // Date represented in milliseconds for date: 2021-09-24T16:40+0000.
  private static final Date DATE = new Date(1632501600000L);
  private DateFieldViewModel dateFieldViewModel;

  @Before
  public void setUp() {
    super.setUp();
    dateFieldViewModel = new DateFieldViewModel(null);
  }

  @Test
  public void testUpdateResponse() {
    dateFieldViewModel.updateResponse(DATE);
    observeUntilFirstChange(dateFieldViewModel.getResponse());
    DateResponse response = (DateResponse) dateFieldViewModel.getResponse().getValue().get();
    assertThat(response.getDate()).isEqualTo(new DateResponse(DATE).getDate());
  }

  @Test
  public void testUpdateResponse_mismatchDate() {
    dateFieldViewModel.updateResponse(DATE);
    observeUntilFirstChange(dateFieldViewModel.getResponse());
    DateResponse response = (DateResponse) dateFieldViewModel.getResponse().getValue().get();
    assertThat(response.getDate()).isNotEqualTo(new DateResponse(new Date()).getDate());
  }

  @Test
  public void testDialogClick() {
    dateFieldViewModel = mock(DateFieldViewModel.class);
    given(dateFieldViewModel.getShowDialogClicks()).willReturn(Observable.just(Nil.NIL));
    Observable<Nil> observable = dateFieldViewModel.getShowDialogClicks();
    TestObserver<Nil> observer = new TestObserver();
    observable.subscribe(observer);

    observer.assertComplete().assertNoErrors().assertValue(Nil.NIL).assertValueCount(1);
  }
}
