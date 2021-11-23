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

package com.google.android.gnd.system;

import static com.google.common.truth.Truth.assertThat;

import com.google.android.gnd.BaseHiltTest;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreException.Code;
import dagger.hilt.android.testing.HiltAndroidTest;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.RobolectricTestRunner;

@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
public class ApplicationErrorManagerTest extends BaseHiltTest {

  /**
   * TODO: Use {@link ParameterizedRobolectricTestRunner} instead of doing it manually. Currently,
   * it fails to initialize {@link FirebaseFirestoreException} needed to generating test input.
   */
  private static final List<Object[]> INPUT_DATA =
      Arrays.asList(
          new Object[][] {
            {new Exception(), false},
            {
              new FirebaseFirestoreException("User not in pass-list", Code.PERMISSION_DENIED),
              true,
              "Permission denied! Check user pass-list."
            }
          });

  @Inject ApplicationErrorManager errorManager;

  @Test
  public void testHandleException() {
    for (Object[] input : INPUT_DATA) {
      Exception exception = (Exception) input[0];
      boolean isConsumed = (boolean) input[1];
      assertThat(errorManager.handleException(exception)).isEqualTo(isConsumed);

      // Expect an error message if the error is consumed.
      if (isConsumed) {
        errorManager.getExceptions().test().assertValue((String) input[2]);
      } else {
        errorManager.getExceptions().test().assertNoValues();
      }
    }
  }
}
