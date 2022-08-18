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

package com.google.android.ground.persistence.remote.firestore.schema;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.android.ground.model.TermsOfService;
import com.google.firebase.firestore.DocumentSnapshot;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TermsOfServiceConverterTest {

  @Mock
  private DocumentSnapshot termsOfServiceDocumentSnapshot;

  private TermsOfService termsOfService;
  private static final String TEST_TERMS = "TERMS";
  private static final String TEST_TERMS_ID = "TERMS_ID";

  @Before
  public void setup() {
    setUpTestTerms();
  }

  @Test
  public void testTermsOfService() {
    mockTermsOfServiceDocumentSnapshot(new TermsOfServiceDocument(TEST_TERMS));
    assertThat(toTermsOfService())
        .isEqualTo(termsOfService);
  }

  private void setUpTestTerms() {
    termsOfService = new TermsOfService(TEST_TERMS_ID, TEST_TERMS);
  }

  /**
   * Mock submission document snapshot to return the specified id and object representation.
   */
  private void mockTermsOfServiceDocumentSnapshot(TermsOfServiceDocument doc) {
    when(termsOfServiceDocumentSnapshot.getId()).thenReturn(TEST_TERMS_ID);
    when(termsOfServiceDocumentSnapshot.toObject(TermsOfServiceDocument.class)).thenReturn(doc);
  }

  private TermsOfService toTermsOfService() {
    return TermsOfServiceConverter.toTerms(termsOfServiceDocumentSnapshot);
  }
}
