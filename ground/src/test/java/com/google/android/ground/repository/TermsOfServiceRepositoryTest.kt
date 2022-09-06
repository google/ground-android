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
package com.google.android.ground.repository

import com.google.android.ground.BaseHiltTest
import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import dagger.hilt.android.testing.HiltAndroidTest
import java8.util.Optional
import javax.inject.Inject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class TermsOfServiceRepositoryTest : BaseHiltTest() {
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore

  @Inject lateinit var termsOfServiceRepository: TermsOfServiceRepository

  @Test
  fun testGetTermsOfService() {
    fakeRemoteDataStore.setTermsOfService(Optional.of(FakeData.TERMS_OF_SERVICE))
    termsOfServiceRepository.termsOfService.test().assertResult(FakeData.TERMS_OF_SERVICE)
  }

  @Test
  fun testGetTermsOfService_whenMissing_doesNotThrowException() {
    fakeRemoteDataStore.setTermsOfService(Optional.empty())
    termsOfServiceRepository.termsOfService.test().assertNoValues().assertComplete()
  }

  @Test
  fun testTermsOfServiceAccepted() {
    termsOfServiceRepository.isTermsOfServiceAccepted = true
    assertThat(termsOfServiceRepository.isTermsOfServiceAccepted).isTrue()
  }

  @Test
  fun testTermsOfServiceNotAccepted() {
    assertThat(termsOfServiceRepository.isTermsOfServiceAccepted).isFalse()
  }
}
