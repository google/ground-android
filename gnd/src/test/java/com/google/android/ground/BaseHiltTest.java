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

package com.google.android.ground;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltTestApplication;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.annotation.Config;

/** Injects Hilt dependencies during setUp. */
@Config(application = HiltTestApplication.class)
public abstract class BaseHiltTest {

  // Required for injecting hilt dependencies using @Inject annotation.
  @Rule public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  // Background executor executes tasks synchronously. Needed for Room database operations.
  @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

  // Allows creating mocks using @Mock annotation.
  @Rule public MockitoRule rule = MockitoJUnit.rule();

  @Before
  @OverridingMethodsMustInvokeSuper
  public void setUp() {
    hiltRule.inject();
  }
}
