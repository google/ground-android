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

package com.google.android.gnd.persistence.remote.firestore.schema;


import static com.google.android.gnd.model.TestModelBuilders.newTermsOfService;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.android.gnd.model.TermsOfService;
import com.google.firebase.firestore.DocumentSnapshot;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TermsOfServiceConverterTest {

  @Mock
  private DocumentSnapshot termsOfServiceDocumentSnapshot;

  private TermsOfService termsOfService;
  private String testTerms = "TERMS";

  @Test
  public void testTermsOfService() {
    setUpTestTerms("tos123");
    mockTermsOfServiceDocumentSnapshot("tos123", new TermsOfServiceDocument(testTerms));
    assertThat(toTermsOfService())
        .isEqualTo(
            TermsOfService.builder()
                .setId("tos123")
                .setText(testTerms)
                .build());
  }

  @Test
  public void testTermsOfService_textMismatch() {
    setUpTestTerms("tos123");
    mockTermsOfServiceDocumentSnapshot("tos123", new TermsOfServiceDocument("demo_terms"));
    assertThat(toTermsOfService().getText())
        .isNotEqualTo(
            TermsOfService.builder()
                .setId("tos123")
                .setText(testTerms)
                .build().getText());
  }

  private void setUpTestTerms(String termsId) {
    termsOfService = newTermsOfService().setId(termsId).setText(testTerms).build();
  }

  /** Mock observation document snapshot to return the specified id and object representation. */
  private void mockTermsOfServiceDocumentSnapshot(String id, TermsOfServiceDocument doc) {
    when(termsOfServiceDocumentSnapshot.getId()).thenReturn(id);
    when(termsOfServiceDocumentSnapshot.toObject(TermsOfServiceDocument.class)).thenReturn(doc);
  }

  private TermsOfService toTermsOfService() {
    return TermsOfServiceConverter.toTerms(termsOfServiceDocumentSnapshot);
  }
}
