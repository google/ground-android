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

package com.google.android.gnd.ui.tos;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.google.android.gnd.persistence.local.LocalDatabaseModule;
import com.google.android.gnd.persistence.local.LocalValueStore;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.repository.TermsOfServiceRepository;
import com.google.android.gnd.rx.SchedulersModule;
import com.google.android.gnd.ui.common.Navigator;
import com.google.common.truth.Truth;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import dagger.hilt.android.testing.UninstallModules;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@HiltAndroidTest
@UninstallModules({SchedulersModule.class, LocalDatabaseModule.class})
@Config(application = HiltTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class TermsOfServiceViewModelTest {

  @Rule public MockitoRule rule = MockitoJUnit.rule();

  @Rule public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  public TermsOfServiceViewModel termsOfServiceViewModel;
  @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
  @Inject Navigator navigator;
  @Inject LocalValueStore localValueStore;
  @Mock RemoteDataStore mockRemoteDataStore;
  private TermsOfServiceRepository termsOfServiceRepository;

  @Before
  public void before() {
    hiltRule.inject();
    termsOfServiceRepository = new TermsOfServiceRepository(mockRemoteDataStore, localValueStore);
    termsOfServiceViewModel = new TermsOfServiceViewModel(navigator, termsOfServiceRepository);
  }

  @Test
  public void testButtonClick() {
    termsOfServiceViewModel.onButtonClicked();
    Truth.assertThat(termsOfServiceRepository.isTermsOfServiceAccepted()).isTrue();
  }

  @Test
  public void testTermsOfServiceText() {
    termsOfServiceViewModel.setTermsOfServiceText("Terms Text");
    Truth.assertThat(termsOfServiceViewModel.getTermsOfServiceText()).isEqualTo("Terms Text");
  }

  @Test
  public void testTermsOfServiceText_mismatch() {
    termsOfServiceViewModel.setTermsOfServiceText("Terms");
    Truth.assertThat(termsOfServiceViewModel.getTermsOfServiceText()).isNotEqualTo("Terms Text");
  }
}
