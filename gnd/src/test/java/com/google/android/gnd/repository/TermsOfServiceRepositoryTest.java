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

import com.google.android.gnd.BaseHiltTest;
import com.google.android.gnd.FakeData;
import com.google.android.gnd.model.TermsOfService;
import dagger.hilt.android.testing.HiltAndroidTest;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
public class TermsOfServiceRepositoryTest extends BaseHiltTest {

  @Inject TermsOfServiceRepository termsOfServiceRepository;

  @Test
  public void testGetTermsOfService() {
    termsOfServiceRepository
        .getTermsOfService()
        .test()
        .assertResult(
            TermsOfService.builder()
                .setId(FakeData.TERMS_OF_SERVICE_ID)
                .setText(FakeData.TERMS_OF_SERVICE)
                .build());
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
