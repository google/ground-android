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

import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Field.Type;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.model.layer.Style;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.local.LocalDatabaseModule;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.persistence.sync.DataSyncWorkManager;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.android.gnd.rx.SchedulersModule;
import com.google.android.gnd.system.auth.AuthenticationManager;
import com.google.common.collect.ImmutableList;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import dagger.hilt.android.testing.UninstallModules;
import io.reactivex.Completable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@SuppressWarnings("unchecked")
@HiltAndroidTest
@UninstallModules({SchedulersModule.class, LocalDatabaseModule.class})
@Config(application = HiltTestApplication.class)
@RunWith(RobolectricTestRunner.class)
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
          .setType(Type.TEXT)
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

  private static final Feature TEST_FEATURE =
      Feature.newBuilder()
          .setId("feature id")
          .setProject(TEST_PROJECT)
          .setLayer(TEST_LAYER)
          .setPoint(TEST_POINT)
          .setCreated(TEST_AUDIT_INFO)
          .setLastModified(TEST_AUDIT_INFO)
          .build();

  @Rule public MockitoRule rule = MockitoJUnit.rule();

  @Rule public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

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

  @Before
  public void setUp() {
    hiltRule.inject();
    featureRepository =
        new FeatureRepository(
            mockLocalDataStore,
            mockRemoteDataStore,
            mockProjectRepository,
            mockWorkManager,
            mockAuthManager,
            mockUuidGenerator);

    mockAuthUser();
  }

  @Test
  public void testCreateFeature() {
    mockApplyAndEnqueue();
    mockEnqueueSyncWorker();

    featureRepository.createFeature(TEST_FEATURE).test().assertNoErrors().assertComplete();

    FeatureMutation actual = captorFeatureMutation.getValue();
    assertThat(actual.getType()).isEqualTo(Mutation.Type.CREATE);
    assertThat(actual.getFeatureId()).isEqualTo(TEST_FEATURE.getId());

    verify(mockLocalDataStore, times(1)).applyAndEnqueue(any(FeatureMutation.class));
    verify(mockWorkManager, times(1)).enqueueSyncWorker(TEST_FEATURE.getId());
  }

  @Test
  public void testUpdateFeature() {
    mockApplyAndEnqueue();
    mockEnqueueSyncWorker();

    featureRepository.updateFeature(TEST_FEATURE).test().assertNoErrors().assertComplete();

    FeatureMutation actual = captorFeatureMutation.getValue();
    assertThat(actual.getType()).isEqualTo(Mutation.Type.UPDATE);
    assertThat(actual.getFeatureId()).isEqualTo(TEST_FEATURE.getId());

    verify(mockLocalDataStore, times(1)).applyAndEnqueue(any(FeatureMutation.class));
    verify(mockWorkManager, times(1)).enqueueSyncWorker(TEST_FEATURE.getId());
  }

  @Test
  public void testDeleteFeature() {
    mockApplyAndEnqueue();
    mockEnqueueSyncWorker();

    featureRepository.deleteFeature(TEST_FEATURE).test().assertNoErrors().assertComplete();

    FeatureMutation actual = captorFeatureMutation.getValue();
    assertThat(actual.getType()).isEqualTo(Mutation.Type.DELETE);
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
        .createFeature(TEST_FEATURE)
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
        .createFeature(TEST_FEATURE)
        .test()
        .assertError(NullPointerException.class)
        .assertNotComplete();

    verify(mockLocalDataStore, times(1)).applyAndEnqueue(any(FeatureMutation.class));
    verify(mockWorkManager, times(1)).enqueueSyncWorker(TEST_FEATURE.getId());
  }
}
