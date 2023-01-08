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
package com.google.android.ground.system

import com.google.android.ground.BaseHiltTest
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.*
import javax.inject.Inject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ApplicationErrorManagerTest : BaseHiltTest() {
  @Inject lateinit var errorManager: ApplicationErrorManager

  @Test
  fun testHandleException() {
    for (input in INPUT_DATA) {
      val exception = input[0] as Exception
      val isConsumed = input[1] as Boolean
      assertThat(errorManager.handleException(exception)).isEqualTo(isConsumed)

      // Expect an error message if the error is consumed.
      if (isConsumed) {
        errorManager.exceptions.test().assertValue(input[2] as String)
      } else {
        errorManager.exceptions.test().assertNoValues()
      }
    }
  }

  companion object {
    /**
     * TODO: Use [ParameterizedRobolectricTestRunner] instead of doing it manually. Currently, it
     * fails to initialize [FirebaseFirestoreException] needed to generating test input.
     */
    private val INPUT_DATA =
      listOf(
        arrayOf<Any>(Exception(), false),
        arrayOf<Any>(
          FirebaseFirestoreException(
            "User not in pass-list",
            FirebaseFirestoreException.Code.PERMISSION_DENIED
          ),
          true,
          "Permission denied! Check user pass-list."
        )
      )
  }
}
