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
package org.groundplatform.android

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.test.junit4.createComposeRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltTestApplication
import javax.annotation.OverridingMethodsMustInvokeSuper
import javax.inject.Inject
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.groundplatform.android.persistence.local.room.LocalDatabase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.annotation.Config

/** Injects Hilt dependencies during setUp. */
@Config(application = HiltTestApplication::class)
open class BaseHiltTest {
  /** Required for injecting hilt dependencies using @Inject annotation. */
  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  /* Background executor executes tasks synchronously. Needed for Room database operations. */
  @get:Rule(order = 1) var instantTaskExecutorRule = InstantTaskExecutorRule()

  /* Allows creating mocks using @Mock annotation. */
  @get:Rule(order = 2) var rule: MockitoRule = MockitoJUnit.rule()

  @get:Rule(order = 3) open val composeTestRule = createComposeRule()

  @Inject lateinit var database: LocalDatabase
  @Inject lateinit var testDispatcher: TestDispatcher

  open fun runWithTestDispatcher(testBody: suspend TestScope.() -> Unit) =
    runTest(context = testDispatcher, testBody = testBody)

  @Before
  @OverridingMethodsMustInvokeSuper
  open fun setUp() {
    hiltRule.inject()
  }

  @After
  fun closeDb() {
    database.close()
  }
}
