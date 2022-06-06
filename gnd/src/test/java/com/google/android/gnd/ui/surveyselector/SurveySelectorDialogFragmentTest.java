/*
 * Copyright 2022 Google LLC
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

package com.google.android.gnd.ui.surveyselector;


import static android.os.Looper.getMainLooper;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.view.View;
import android.widget.ListView;
import com.google.android.gnd.BaseHiltTest;
import com.google.android.gnd.FakeData;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.model.Survey;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.local.LocalDataStoreModule;
import com.google.android.gnd.persistence.remote.FakeRemoteDataStore;
import com.google.android.gnd.repository.SurveyRepository;
import com.google.common.collect.ImmutableList;
import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.UninstallModules;
import io.reactivex.Maybe;
import java.util.List;
import java8.util.Optional;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
@UninstallModules({LocalDataStoreModule.class})
public class SurveySelectorDialogFragmentTest extends BaseHiltTest {

  @Inject
  SurveyRepository surveyRepository;
  @Inject FakeRemoteDataStore fakeRemoteDataStore;
  @BindValue @Mock LocalDataStore mockLocalDataStore;

  private SurveySelectorDialogFragment surveySelectorDialogFragment;

  private final Survey survey1 = FakeData.newSurvey().setId("1").build();
  private final Survey survey2 = FakeData.newSurvey().setId("2").build();

  private final List<Survey> surveys = ImmutableList.of(survey1, survey2);

  @Before
  public void setUp() {
    super.setUp();
    fakeRemoteDataStore.setTestSurveys(surveys);
    setUpFragment();
  }

  private void setUpFragment() {
    ActivityController<MainActivity> activityController =
        Robolectric.buildActivity(MainActivity.class);
    MainActivity activity = activityController.setup().get();

    surveySelectorDialogFragment = new SurveySelectorDialogFragment();

    surveySelectorDialogFragment.showNow(activity.getSupportFragmentManager(),
        SurveySelectorDialogFragment.class.getSimpleName());
    shadowOf(getMainLooper()).idle();
  }

  @Test
  public void show_surveyDialogIsShown() {
    View listView = surveySelectorDialogFragment.getDialog().getCurrentFocus();

    assertThat(listView).isNotNull();
    assertThat(listView.getVisibility()).isEqualTo(View.VISIBLE);
    assertThat(listView.findViewById(R.id.survey_name).getVisibility()).isEqualTo(View.VISIBLE);
  }

  @Test
  public void show_surveySelected_surveyIsActivated() {
    ListView listView = (ListView) surveySelectorDialogFragment.getDialog().getCurrentFocus();

    when(mockLocalDataStore.getSurveyById(eq(survey2.getId()))).thenReturn(Maybe.just(survey2));
    shadowOf(listView).performItemClick(1);
    shadowOf(getMainLooper()).idle();

    // Verify Dialog is dismissed
    assertThat(surveySelectorDialogFragment.getDialog()).isNull();
    surveyRepository.getActiveSurvey().test().assertValue(Optional.of(survey2));
  }
}
