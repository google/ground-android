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

package com.google.android.gnd.repository;

import static com.google.android.gnd.FakeData.newJob;
import static com.google.android.gnd.FakeData.newSurvey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.android.gnd.BaseHiltTest;
import com.google.android.gnd.model.Role;
import com.google.android.gnd.model.Survey;
import com.google.android.gnd.model.feature.FeatureType;
import com.google.android.gnd.model.job.Job;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.local.LocalDataStoreModule;
import com.google.android.gnd.persistence.remote.FakeRemoteDataStore;
import com.google.common.collect.ImmutableList;
import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.UninstallModules;
import io.reactivex.Maybe;
import java8.util.Optional;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

@HiltAndroidTest
@UninstallModules({LocalDataStoreModule.class})
@RunWith(RobolectricTestRunner.class)
public class SurveyRepositoryTest extends BaseHiltTest {

  @BindValue @Mock LocalDataStore mockLocalDataStore;
  @BindValue @Mock UserRepository userRepository;

  @Inject
  SurveyRepository surveyRepository;
  @Inject FakeRemoteDataStore fakeRemoteDataStore;

  @Test
  public void testActivateSurvey() {
    Survey survey = newSurvey().build();
    setTestSurvey(survey);

    surveyRepository.activateSurvey("id");

    surveyRepository.getActiveSurvey().test().assertValue(Optional.of(survey));
  }

  @Test
  public void testActivateSurvey_managersCanAddFeaturesToAllJobs() {
    Job job = newJob().setId("job").build();
    setTestSurvey(newSurvey().putJob(job).build());
    when(userRepository.getUserRole(any())).thenReturn(Role.SURVEY_ORGANIZER);

    surveyRepository.activateSurvey("id");

    Job expectedJob = job.toBuilder().setUserCanAdd(FeatureType.ALL).build();
    surveyRepository
        .getActiveSurvey()
        .test()
        .assertValue(p -> p.get().getJobs().equals(ImmutableList.of(expectedJob)));
  }

  @Test
  public void testActivateSurvey_ownersCanAddFeaturesToAllJobs() {
    Job job = newJob().setId("job").build();
    setTestSurvey(newSurvey().putJob(job).build());
    when(userRepository.getUserRole(any())).thenReturn(Role.OWNER);

    surveyRepository.activateSurvey("id");

    Job expectedJob = job.toBuilder().setUserCanAdd(FeatureType.ALL).build();
    surveyRepository
        .getActiveSurvey()
        .test()
        .assertValue(p -> p.get().getJobs().equals(ImmutableList.of(expectedJob)));
  }

  @Test
  public void testActivateSurvey_contributorsCannotAddFeaturesToAnyJobs() {
    Job job = newJob().setId("job").build();
    setTestSurvey(newSurvey().putJob(job).build());
    when(userRepository.getUserRole(any())).thenReturn(Role.DATA_COLLECTOR);

    surveyRepository.activateSurvey("id");

    Job expectedJob = job.toBuilder().setUserCanAdd(ImmutableList.of()).build();
    surveyRepository
        .getActiveSurvey()
        .test()
        .assertValue(p -> p.get().getJobs().equals(ImmutableList.of(expectedJob)));
  }

  private void setTestSurvey(Survey survey) {
    fakeRemoteDataStore.setTestSurvey(survey);
    when(mockLocalDataStore.getSurveyById(any())).thenReturn(Maybe.just(survey));
  }
}
