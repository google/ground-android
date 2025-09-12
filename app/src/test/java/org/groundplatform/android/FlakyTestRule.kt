/*
 * Copyright 2025 Google LLC
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

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A JUnit rule that retries tests annotated with [FlakyTest] a specified number of times before
 * failing. This is useful for tests that may occasionally fail due to external factors, such as
 * network issues or race conditions.
 */
class FlakyTestRule : TestRule {
  override fun apply(base: Statement, description: Description): Statement =
    object : Statement() {
      override fun evaluate() {
        val annotation = description.getAnnotation(FlakyTest::class.java)
        val maxAttempts = (annotation?.retryCount ?: 0) + 1

        for (attempt in 1..maxAttempts) {
          try {
            base.evaluate()
            return
          } catch (throwable: Throwable) {
            if (attempt == maxAttempts) {
              throw throwable
            }
          }
        }
      }
    }
}
