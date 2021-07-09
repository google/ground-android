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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.model.observation.TimeResponse;
import com.google.android.gnd.persistence.local.LocalDatabaseModule;
import com.google.android.gnd.rx.Nil;
import com.google.android.gnd.rx.SchedulersModule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import dagger.hilt.android.testing.UninstallModules;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import java.util.Date;
import java8.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@HiltAndroidTest
@UninstallModules({SchedulersModule.class, LocalDatabaseModule.class})
@Config(application = HiltTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class TimeFieldViewModelTest {

  private TimeFieldViewModel timeFieldViewModel;
  //Date represented in milliseconds for date: 2021-09-24T16:40+0000.
  public static final Date DATE = new Date(1632501600000L);

  @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

  @Before
  public void setup() {
    timeFieldViewModel = new TimeFieldViewModel(null);
  }

  @Test
  public void testUpdateResponse() {
    timeFieldViewModel.updateResponse(DATE);
    // Observer initialized to read the value of live data.
    timeFieldViewModel.getResponse().observeForever(new Observer<Optional<Response>>() {
      @Override
      public void onChanged(
          Optional<Response> responseOptional) {
        timeFieldViewModel.getResponse().removeObserver(this);
      }
    });
    TimeResponse response = (TimeResponse) timeFieldViewModel.getResponse().getValue().get();
    assertThat(response.getTime())
        .isEqualTo(new TimeResponse(DATE).getTime());
  }

  @Test
  public void testUpdateResponse_mismatchTime() {
    timeFieldViewModel.updateResponse(DATE);
    // Observer initialized to read the value of live data.
    timeFieldViewModel.getResponse().observeForever(new Observer<Optional<Response>>() {
      @Override
      public void onChanged(
          Optional<Response> responseOptional) {
        timeFieldViewModel.getResponse().removeObserver(this);
      }
    });
    TimeResponse response = (TimeResponse) timeFieldViewModel.getResponse().getValue().get();
    assertThat(response.getTime())
        .isNotEqualTo(new TimeResponse(new Date()).getTime());
  }


  @Test
  public void testDialogClick() {
    timeFieldViewModel = mock(TimeFieldViewModel.class);
    given(timeFieldViewModel.getShowDialogClicks()).willReturn(Observable.just(Nil.NIL));
    Observable<Nil> observable = timeFieldViewModel.getShowDialogClicks();
    TestObserver<Nil> observer  = new TestObserver();
    observable.subscribe(observer);

    observer.assertComplete()
        .assertNoErrors()
        .assertValue(Nil.NIL).assertValueCount(1);
  }
}
