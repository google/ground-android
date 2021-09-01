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

package com.google.android.gnd.repository;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.Mutation.SyncStatus;
import com.google.android.gnd.model.Mutation.Type;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.feature.PointFeature;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.model.layer.Style;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.remote.RemoteDataEvent;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.persistence.sync.DataSyncWorkManager;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.android.gnd.system.auth.AuthenticationManager;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.Date;
import java.util.NoSuchElementException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

// TODO: Include a test for Polygon feature
@RunWith(PowerMockRunner.class)
@PrepareForTest(FeatureRepository.class) // Needed for mocking "new Date()"
public class FeatureRepositoryTest {

  private static final User TEST_USER =
      User.builder().setId("user id").setEmail("user@gmail.com").setDisplayName("user 1").build();

  private static final AuditInfo TEST_AUDIT_INFO = AuditInfo.now(TEST_USER);

  private static final Point TEST_POINT =
      Point.newBuilder().setLatitude(110.0).setLongitude(-23.1).build();

  private static final Field TEST_FIELD =
      Field.newBuilder()
          .setId("field id")
          .setIndex(1)
          .setLabel("field label")
          .setRequired(false)
          .setType(Field.Type.TEXT_FIELD)
          .build();

  private static final Form TEST_FORM =
      Form.newBuilder()
          .setId("form id")
          .setElements(ImmutableList.of(Element.ofField(TEST_FIELD)))
          .build();

  private static final Layer TEST_LAYER =
      Layer.newBuilder()
          .setId("layer id")
          .setName("heading title")
          .setDefaultStyle(Style.builder().setColor("000").build())
          .setForm(TEST_FORM)
          .build();

  private static final Project TEST_PROJECT =
      Project.newBuilder()
          .setId("project id")
          .setTitle("project 1")
          .setDescription("foo description")
          .putLayer("layer id", TEST_LAYER)
          .build();

  private static final PointFeature TEST_FEATURE =
      PointFeature.newBuilder()
          .setId("feature id")
          .setProject(TEST_PROJECT)
          .setLayer(TEST_LAYER)
          .setPoint(TEST_POINT)
          .setCreated(TEST_AUDIT_INFO)
          .setLastModified(TEST_AUDIT_INFO)
          .build();

  private static final Date FAKE_NOW = new Date();

  @Rule public MockitoRule rule = MockitoJUnit.rule();

  @Mock LocalDataStore mockLocalDataStore;
  @Mock RemoteDataStore mockRemoteDataStore;
  @Mock ProjectRepository mockProjectRepository;
  @Mock DataSyncWorkManager mockWorkManager;
  @Mock AuthenticationManager mockAuthManager;
  @Mock OfflineUuidGenerator mockUuidGenerator;

  @Captor ArgumentCaptor<FeatureMutation> captorFeatureMutation;

  private FeatureRepository featureRepository;

  private void mockAuthUser() {
    doReturn(TEST_USER).when(mockAuthManager).getCurrentUser();
  }

  private void mockApplyAndEnqueue() {
    doReturn(Completable.complete())
        .when(mockLocalDataStore)
        .applyAndEnqueue(captorFeatureMutation.capture());
  }

  private void mockEnqueueSyncWorker() {
    doReturn(Completable.complete()).when(mockWorkManager).enqueueSyncWorker(anyString());
  }

  private void mockRemoteFeatureStream(RemoteDataEvent<Feature> event) {
    when(mockRemoteDataStore.loadFeaturesOnceAndStreamChanges(TEST_PROJECT))
        .thenReturn(Flowable.just(event));
  }

  @Before
  public void setUp() {
    featureRepository =
        new FeatureRepository(
            mockLocalDataStore,
            mockRemoteDataStore,
            mockProjectRepository,
            mockWorkManager,
            mockAuthManager,
            mockUuidGenerator);
  }

  @Test
  public void testApplyAndEnqueue() {
    mockApplyAndEnqueue();
    mockEnqueueSyncWorker();

    featureRepository
        .applyAndEnqueue(TEST_FEATURE.toMutation(Type.CREATE, TEST_USER.getId()))
        .test()
        .assertNoErrors()
        .assertComplete();

    FeatureMutation actual = captorFeatureMutation.getValue();
    assertThat(actual.getType()).isEqualTo(Mutation.Type.CREATE);
    assertThat(actual.getSyncStatus()).isEqualTo(SyncStatus.PENDING);
    assertThat(actual.getFeatureId()).isEqualTo(TEST_FEATURE.getId());

    verify(mockLocalDataStore, times(1)).applyAndEnqueue(any(FeatureMutation.class));
    verify(mockWorkManager, times(1)).enqueueSyncWorker(TEST_FEATURE.getId());
  }

  @Test
  public void testApplyAndEnqueue_returnsError() {
    mockEnqueueSyncWorker();

    doReturn(Completable.error(new NullPointerException()))
        .when(mockLocalDataStore)
        .applyAndEnqueue(any(FeatureMutation.class));

    featureRepository
        .applyAndEnqueue(TEST_FEATURE.toMutation(Type.CREATE, TEST_USER.getId()))
        .test()
        .assertError(NullPointerException.class)
        .assertNotComplete();

    verify(mockLocalDataStore, times(1)).applyAndEnqueue(any(FeatureMutation.class));
    verify(mockWorkManager, times(1)).enqueueSyncWorker(TEST_FEATURE.getId());
  }

  @Test
  public void testEnqueueSyncWorker_returnsError() {
    mockApplyAndEnqueue();

    doReturn(Completable.error(new NullPointerException()))
        .when(mockWorkManager)
        .enqueueSyncWorker(anyString());

    featureRepository
        .applyAndEnqueue(TEST_FEATURE.toMutation(Type.CREATE, TEST_USER.getId()))
        .test()
        .assertError(NullPointerException.class)
        .assertNotComplete();

    verify(mockLocalDataStore, times(1)).applyAndEnqueue(any(FeatureMutation.class));
    verify(mockWorkManager, times(1)).enqueueSyncWorker(TEST_FEATURE.getId());
  }

  @Test
  public void testSyncFeatures_loaded() {
    mockRemoteFeatureStream(RemoteDataEvent.loaded("entityId", TEST_FEATURE));
    when(mockLocalDataStore.mergeFeature(TEST_FEATURE)).thenReturn(Completable.complete());

    featureRepository.syncFeatures(TEST_PROJECT).test().assertNoErrors().assertComplete();

    verify(mockLocalDataStore, times(1)).mergeFeature(TEST_FEATURE);
  }

  @Test
  public void testSyncFeatures_modified() {
    mockRemoteFeatureStream(RemoteDataEvent.modified("entityId", TEST_FEATURE));
    when(mockLocalDataStore.mergeFeature(TEST_FEATURE)).thenReturn(Completable.complete());

    featureRepository.syncFeatures(TEST_PROJECT).test().assertNoErrors().assertComplete();

    verify(mockLocalDataStore, times(1)).mergeFeature(TEST_FEATURE);
  }

  @Test
  public void testSyncFeatures_removed() {
    mockRemoteFeatureStream(RemoteDataEvent.removed("entityId"));
    when(mockLocalDataStore.deleteFeature(anyString())).thenReturn(Completable.complete());

    featureRepository.syncFeatures(TEST_PROJECT).test().assertComplete();

    verify(mockLocalDataStore, times(1)).deleteFeature("entityId");
  }

  @Test
  public void testSyncFeatures_error() {
    mockRemoteFeatureStream(RemoteDataEvent.error(new Throwable("Foo error")));
    featureRepository.syncFeatures(TEST_PROJECT).test().assertNoErrors().assertComplete();
  }

  @Test
  public void testGetFeaturesOnceAndStream() {
    when(mockLocalDataStore.getFeaturesOnceAndStream(TEST_PROJECT))
        .thenReturn(Flowable.just(ImmutableSet.of(TEST_FEATURE)));

    featureRepository
        .getFeaturesOnceAndStream(TEST_PROJECT)
        .test()
        .assertValue(ImmutableSet.of(TEST_FEATURE));
  }

  @Test
  public void testGetFeature_projectNotPresent() {
    when(mockProjectRepository.getProject(anyString()))
        .thenReturn(Single.error(new NoSuchElementException()));

    featureRepository
        .getFeature("non_existent_project_id", "feature_id")
        .test()
        .assertFailure(NoSuchElementException.class);
  }

  @Test
  public void testGetFeature_projectPresent() {
    when(mockProjectRepository.getProject(anyString())).thenReturn(Single.just(TEST_PROJECT));
    when(mockLocalDataStore.getFeature(TEST_PROJECT, TEST_FEATURE.getId()))
        .thenReturn(Maybe.just(TEST_FEATURE));

    featureRepository
        .getFeature(TEST_PROJECT.getId(), TEST_FEATURE.getId())
        .test()
        .assertResult(TEST_FEATURE);

    featureRepository
        .getFeature(TEST_FEATURE.toMutation(Type.UPDATE, "user_id"))
        .test()
        .assertResult(TEST_FEATURE);
  }

  @Test
  public void testNewFeature() throws Exception {
    mockAuthUser();
    when(mockUuidGenerator.generateUuid()).thenReturn("new_uuid");
    whenNew(Date.class).withNoArguments().thenReturn(FAKE_NOW);

    FeatureMutation newMutation =
        featureRepository.newMutation("foo_project_id", "foo_layer_id", TEST_POINT);

    assertThat(newMutation.getId()).isNull();
    assertThat(newMutation.getFeatureId()).isEqualTo("new_uuid");
    assertThat(newMutation.getProjectId()).isEqualTo("foo_project_id");
    assertThat(newMutation.getLayerId()).isEqualTo("foo_layer_id");
    assertThat(newMutation.getNewLocation().get()).isEqualTo(TEST_POINT);
    assertThat(newMutation.getUserId()).isEqualTo(TEST_USER.getId());
    assertThat(newMutation.getClientTimestamp()).isEqualTo(FAKE_NOW);
  }
}
