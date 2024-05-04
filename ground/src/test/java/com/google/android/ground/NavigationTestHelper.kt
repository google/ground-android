/*
 * Copyright 2023 Google LLC
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
package com.google.android.ground

import androidx.navigation.NavDirections
import app.cash.turbine.test
import com.google.android.ground.ui.common.NavigateTo
import com.google.android.ground.ui.common.NavigationRequest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle

/**
 * Asserts that the given [expectedNavDirections] nav direction is emitted by [testSharedFlow] when
 * the given block [runOnSubscription] is invoked.
 *
 * @param testSharedFlow SharedFlow under test
 * @param expectedNavDirections NavDirections to validate after the flow has been subscribed
 * @param runOnSubscription Runs the block when the given flow is subscribed
 */
suspend fun TestScope.testNavigateTo(
  testSharedFlow: SharedFlow<NavigationRequest>,
  expectedNavDirections: NavDirections,
  runOnSubscription: () -> Unit,
) {
  testNavigateTo(
    testSharedFlow,
    { navDirections -> assertThat(navDirections).isEqualTo(expectedNavDirections) },
    runOnSubscription,
  )
}

suspend fun TestScope.testNavigateTo(
  testSharedFlow: SharedFlow<NavigationRequest>,
  validate: (NavDirections) -> Unit,
  runOnSubscription: () -> Unit,
) {
  testNavigate(
    testSharedFlow,
    { navigationRequest ->
      assertThat(navigationRequest).isInstanceOf(NavigateTo::class.java)
      validate((navigationRequest as NavigateTo).directions)
    },
    runOnSubscription,
  )
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun TestScope.testNavigate(
  testSharedFlow: SharedFlow<NavigationRequest>,
  validate: (NavigationRequest) -> Unit,
  runOnSubscription: () -> Unit,
) {
  testSharedFlow
    .onSubscription {
      runOnSubscription()
      advanceUntilIdle()
    }
    .test { validate(expectMostRecentItem()) }
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun TestScope.testNoNavigation(
  testSharedFlow: SharedFlow<NavigationRequest>,
  runOnSubscription: () -> Unit,
) {
  testSharedFlow
    .onSubscription {
      runOnSubscription()
      advanceUntilIdle()
    }
    .test { expectNoEvents() }
}

/**
 * Verifies that the given [NavDirections] is emitted if [expectedNavDirections] is provided,
 * otherwise asserts that [NavDirections] should be emitted.
 */
suspend fun TestScope.testMaybeNavigateTo(
  testSharedFlow: SharedFlow<NavigationRequest>,
  expectedNavDirections: NavDirections?,
  runOnSubscription: () -> Unit,
) {
  if (expectedNavDirections == null) {
    testNoNavigation(testSharedFlow, runOnSubscription)
  } else {
    testNavigateTo(testSharedFlow, expectedNavDirections, runOnSubscription)
  }
}
