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

import com.google.android.gnd.model.TermsOfService;
import com.google.android.gnd.persistence.remote.DataStoreException;
import com.google.firebase.firestore.DocumentSnapshot;

/** Converts between Firestore documents and {@link TermsOfService} instances. */
public class TermsOfServiceConverter {

  static TermsOfService toTerms(DocumentSnapshot doc) throws DataStoreException {
    TermsofServiceDocument pd = doc.toObject(TermsofServiceDocument.class);
    TermsOfService.Builder terms = TermsOfService.builder();
    terms.setId(doc.getId()).setText(pd.getText());
    return terms.build();
  }
}
