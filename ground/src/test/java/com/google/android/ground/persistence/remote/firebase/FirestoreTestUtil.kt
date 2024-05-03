/*
 * Copyright 2024 Google LLC
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

package com.google.android.ground.persistence.remote.firebase

import com.google.firebase.firestore.DocumentSnapshot
import org.mockito.Mockito
import org.mockito.kotlin.whenever

/**
 * Returns a new mock [DocumentSnapshot] for testing which provides the specified [id] and [data].
 */
fun newDocumentSnapshot(id: String = "", data: Map<String, Any>? = null): DocumentSnapshot {
  val mockSnapshot = Mockito.mock(DocumentSnapshot::class.java)
  whenever(mockSnapshot.id).thenReturn(id)
  whenever(mockSnapshot.data).thenReturn(data)
  return mockSnapshot
}
