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

package org.groundplatform.android.persistence.remote.firebase.schema

import com.google.firebase.firestore.DocumentSnapshot
import org.groundplatform.android.model.TermsOfService

/** Converts between Firestore documents and [TermsOfService] instances. */
object TermsOfServiceConverter {

  fun toTerms(doc: DocumentSnapshot): TermsOfService? {
    if (!doc.exists()) return null
    val pd = doc.toObject(TermsOfServiceDocument::class.java)
    return TermsOfService(doc.id, pd!!.text)
  }
}
