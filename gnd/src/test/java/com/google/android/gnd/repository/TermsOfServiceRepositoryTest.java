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

import static com.google.common.truth.Truth.assertThat;

import com.google.android.gnd.HiltTestWithRobolectricRunner;
import com.google.android.gnd.persistence.local.LocalValueStore;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import dagger.hilt.android.testing.HiltAndroidTest;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

@HiltAndroidTest
public class TermsOfServiceRepositoryTest extends HiltTestWithRobolectricRunner {

  @Inject LocalValueStore localValueStore;
  @Mock RemoteDataStore mockRemoteDataStore;

  private TermsOfServiceRepository termsOfServiceRepository;

  @Before
  public void setUp() {
    super.setUp();
    termsOfServiceRepository = new TermsOfServiceRepository(mockRemoteDataStore, localValueStore);
  }

  @Test
  public void testTermsOfServiceAccepted() {
    termsOfServiceRepository.setTermsOfServiceAccepted(true);
    assertThat(termsOfServiceRepository.isTermsOfServiceAccepted()).isTrue();
  }

  @Test
  public void testTermsOfServiceNotAccepted() {
    assertThat(termsOfServiceRepository.isTermsOfServiceAccepted()).isFalse();
  }
}
