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

package com.google.android.ground.repository;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.ground.BaseHiltTest;
import com.google.android.ground.model.mutation.LocationOfInterestMutation;
import com.google.android.ground.model.mutation.Mutation;
import com.google.android.ground.model.mutation.Mutation.SyncStatus;
import com.google.android.ground.model.mutation.Mutation.Type;
import com.google.android.ground.persistence.local.LocalDataStore;
import com.google.android.ground.persistence.local.LocalDataStoreModule;
import com.google.android.ground.persistence.local.room.models.MutationEntitySyncStatus;
import com.google.android.ground.persistence.remote.NotFoundException;
import com.google.android.ground.persistence.remote.RemoteDataEvent;
import com.google.android.ground.persistence.sync.DataSyncWorkManager;
import com.google.android.ground.test.FakeData;
import com.google.android.ground.test.persistence.remote.FakeRemoteDataStore;
import com.google.android.ground.test.system.auth.FakeAuthenticationManager;
import com.google.common.collect.ImmutableSet;
import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.UninstallModules;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.Date;
import java.util.NoSuchElementException;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

// TODO: Include a test for Polygon locationOfInterest
@HiltAndroidTest
@UninstallModules({LocalDataStoreModule.class})
@RunWith(RobolectricTestRunner.class)
public class LocationOfInterestRepositoryTest extends BaseHiltTest {
  @BindValue @Mock LocalDataStore mockLocalDataStore;
  @BindValue @Mock SurveyRepository mockSurveyRepository;
  @BindValue @Mock DataSyncWorkManager mockWorkManager;

  @Captor ArgumentCaptor<LocationOfInterestMutation> captorFeatureMutation;

  @Inject FakeAuthenticationManager fakeAuthenticationManager;
  @Inject FakeRemoteDataStore fakeRemoteDataStore;
  @Inject LocationOfInterestRepository locationOfInterestRepository;

  private void mockApplyAndEnqueue() {
    doReturn(Completable.complete())
        .when(mockLocalDataStore)
        .applyAndEnqueue(captorFeatureMutation.capture());
  }

  private void mockEnqueueSyncWorker() {
    when(mockWorkManager.enqueueSyncWorker(anyString())).thenReturn(Completable.complete());
  }

  @Test
  public void testApplyAndEnqueue() {
    mockApplyAndEnqueue();
    mockEnqueueSyncWorker();

    locationOfInterestRepository
        .applyAndEnqueue(FakeData.POINT_OF_INTEREST.toMutation(Type.CREATE, FakeData.USER.getId()))
        .test()
        .assertNoErrors()
        .assertComplete();

    LocationOfInterestMutation actual = captorFeatureMutation.getValue();
    assertThat(actual.getType()).isEqualTo(Mutation.Type.CREATE);
    assertThat(actual.getSyncStatus()).isEqualTo(SyncStatus.PENDING);
    assertThat(actual.getLocationOfInterestId()).isEqualTo(FakeData.POINT_OF_INTEREST.getId());

    verify(mockLocalDataStore, times(1)).applyAndEnqueue(any(LocationOfInterestMutation.class));
    verify(mockWorkManager, times(1)).enqueueSyncWorker(FakeData.POINT_OF_INTEREST.getId());
  }

  @Test
  public void testApplyAndEnqueue_returnsError() {
    mockEnqueueSyncWorker();

    doReturn(Completable.error(new NullPointerException()))
        .when(mockLocalDataStore)
        .applyAndEnqueue(any(LocationOfInterestMutation.class));

    locationOfInterestRepository
        .applyAndEnqueue(FakeData.POINT_OF_INTEREST.toMutation(Type.CREATE, FakeData.USER.getId()))
        .test()
        .assertError(NullPointerException.class)
        .assertNotComplete();

    verify(mockLocalDataStore, times(1)).applyAndEnqueue(any(LocationOfInterestMutation.class));
    verify(mockWorkManager, times(1)).enqueueSyncWorker(FakeData.POINT_OF_INTEREST.getId());
  }

  @Test
  public void testEnqueueSyncWorker_returnsError() {
    mockApplyAndEnqueue();

    when(mockWorkManager.enqueueSyncWorker(anyString()))
        .thenReturn(Completable.error(new NullPointerException()));

    locationOfInterestRepository
        .applyAndEnqueue(FakeData.POINT_OF_INTEREST.toMutation(Type.CREATE, FakeData.USER.getId()))
        .test()
        .assertError(NullPointerException.class)
        .assertNotComplete();

    verify(mockLocalDataStore, times(1)).applyAndEnqueue(any(LocationOfInterestMutation.class));
    verify(mockWorkManager, times(1)).enqueueSyncWorker(FakeData.POINT_OF_INTEREST.getId());
  }

  @Test
  public void testSyncFeatures_loaded() {
    fakeRemoteDataStore.streamFeatureOnce(
        RemoteDataEvent.loaded("entityId", FakeData.POINT_OF_INTEREST));
    when(mockLocalDataStore.mergeLocationOfInterest(FakeData.POINT_OF_INTEREST))
        .thenReturn(Completable.complete());

    locationOfInterestRepository
        .syncLocationsOfInterest(FakeData.SURVEY)
        .test()
        .assertNoErrors()
        .assertComplete();

    verify(mockLocalDataStore, times(1)).mergeLocationOfInterest(FakeData.POINT_OF_INTEREST);
  }

  @Test
  public void testSyncFeatures_modified() {
    fakeRemoteDataStore.streamFeatureOnce(
        RemoteDataEvent.modified("entityId", FakeData.POINT_OF_INTEREST));
    when(mockLocalDataStore.mergeLocationOfInterest(FakeData.POINT_OF_INTEREST))
        .thenReturn(Completable.complete());

    locationOfInterestRepository
        .syncLocationsOfInterest(FakeData.SURVEY)
        .test()
        .assertNoErrors()
        .assertComplete();

    verify(mockLocalDataStore, times(1)).mergeLocationOfInterest(FakeData.POINT_OF_INTEREST);
  }

  @Test
  public void testSyncFeatures_removed() {
    fakeRemoteDataStore.streamFeatureOnce(RemoteDataEvent.removed("entityId"));
    when(mockLocalDataStore.deleteLocationOfInterest(anyString()))
        .thenReturn(Completable.complete());

    locationOfInterestRepository.syncLocationsOfInterest(FakeData.SURVEY).test().assertComplete();

    verify(mockLocalDataStore, times(1)).deleteLocationOfInterest("entityId");
  }

  @Test
  public void testSyncFeatures_error() {
    fakeRemoteDataStore.streamFeatureOnce(RemoteDataEvent.error(new Throwable("Foo error")));
    locationOfInterestRepository
        .syncLocationsOfInterest(FakeData.SURVEY)
        .test()
        .assertNoErrors()
        .assertComplete();
  }

  @Test
  public void testGetFeaturesOnceAndStream() {
    when(mockLocalDataStore.getLocationsOfInterestOnceAndStream(FakeData.SURVEY))
        .thenReturn(Flowable.just(ImmutableSet.of(FakeData.POINT_OF_INTEREST)));

    locationOfInterestRepository
        .getLocationsOfInterestOnceAndStream(FakeData.SURVEY)
        .test()
        .assertValue(ImmutableSet.of(FakeData.POINT_OF_INTEREST));
  }

  @Test
  public void testGetFeature_surveyNotPresent() {
    when(mockSurveyRepository.getSurvey(anyString()))
        .thenReturn(Single.error(new NoSuchElementException()));

    locationOfInterestRepository
        .getLocationOfInterest("non_existent_survey_id", "feature_id")
        .test()
        .assertFailure(NoSuchElementException.class);
  }

  @Test
  public void testGetFeature_surveyPresent() {
    when(mockSurveyRepository.getSurvey(anyString())).thenReturn(Single.just(FakeData.SURVEY));
    when(mockLocalDataStore.getLocationOfInterest(
            FakeData.SURVEY, FakeData.POINT_OF_INTEREST.getId()))
        .thenReturn(Maybe.just(FakeData.POINT_OF_INTEREST));

    locationOfInterestRepository
        .getLocationOfInterest(FakeData.SURVEY.getId(), FakeData.POINT_OF_INTEREST.getId())
        .test()
        .assertResult(FakeData.POINT_OF_INTEREST);

    locationOfInterestRepository
        .getLocationOfInterest(FakeData.POINT_OF_INTEREST.toMutation(Type.UPDATE, "user_id"))
        .test()
        .assertResult(FakeData.POINT_OF_INTEREST);
  }

  @Test
  public void testGetLocationOfInterest_whenLocationOfInterestIsNotPresent() {
    when(mockSurveyRepository.getSurvey(anyString())).thenReturn(Single.just(FakeData.SURVEY));
    when(mockLocalDataStore.getLocationOfInterest(
            FakeData.SURVEY, FakeData.POINT_OF_INTEREST.getId()))
        .thenReturn(Maybe.empty());

    locationOfInterestRepository
        .getLocationOfInterest(FakeData.SURVEY.getId(), FakeData.POINT_OF_INTEREST.getId())
        .test()
        .assertFailureAndMessage(NotFoundException.class, "Location of interest not found loi id");
  }

  @Test
  public void testNewLocationOfInterest() {
    fakeAuthenticationManager.setUser(FakeData.USER);
    Date testDate = new Date();

    LocationOfInterestMutation newMutation =
        locationOfInterestRepository.newMutation(
            "foo_survey_id", "foo_job_id", FakeData.POINT, testDate);

    assertThat(newMutation.getId()).isNull();
    assertThat(newMutation.getLocationOfInterestId()).isEqualTo("TEST UUID");
    assertThat(newMutation.getSurveyId()).isEqualTo("foo_survey_id");
    assertThat(newMutation.getJobId()).isEqualTo("foo_job_id");
    assertThat(newMutation.getLocation().get()).isEqualTo(FakeData.POINT);
    assertThat(newMutation.getUserId()).isEqualTo(FakeData.USER.getId());
    assertThat(newMutation.getClientTimestamp()).isEqualTo(testDate);
  }

  @Test
  public void testNewPolygonOfInterest() {
    fakeAuthenticationManager.setUser(FakeData.USER);
    Date testDate = new Date();

    LocationOfInterestMutation newMutation =
        locationOfInterestRepository.newPolygonOfInterestMutation(
            "foo_survey_id", "foo_job_id", FakeData.VERTICES, testDate);

    assertThat(newMutation.getId()).isNull();
    assertThat(newMutation.getLocationOfInterestId()).isEqualTo("TEST UUID");
    assertThat(newMutation.getSurveyId()).isEqualTo("foo_survey_id");
    assertThat(newMutation.getJobId()).isEqualTo("foo_job_id");
    assertThat(newMutation.getPolygonVertices()).isEqualTo(FakeData.VERTICES);
    assertThat(newMutation.getUserId()).isEqualTo(FakeData.USER.getId());
    assertThat(newMutation.getClientTimestamp()).isEqualTo(testDate);
  }

  @Test
  public void testGetIncompleteFeatureMutationsOnceAndStream() {
    locationOfInterestRepository.getIncompleteLocationOfInterestMutationsOnceAndStream(
        "feature_id_1");

    verify(mockLocalDataStore, times(1))
        .getLocationOfInterestMutationsByLocationOfInterestIdOnceAndStream(
            "feature_id_1",
            MutationEntitySyncStatus.PENDING,
            MutationEntitySyncStatus.IN_PROGRESS,
            MutationEntitySyncStatus.FAILED);
  }

  @Test
  public void testPolygonInfoDialogShown() {
    assertThat(locationOfInterestRepository.isPolygonInfoDialogShown()).isFalse();

    locationOfInterestRepository.setPolygonDialogInfoShown(true);
    assertThat(locationOfInterestRepository.isPolygonInfoDialogShown()).isTrue();

    locationOfInterestRepository.setPolygonDialogInfoShown(false);
    assertThat(locationOfInterestRepository.isPolygonInfoDialogShown()).isFalse();
  }
}
