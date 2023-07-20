/*
 * Copyright 2023 Google LLC
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

package com.google.android.ground.persistence.remote.firebase.schema

import com.google.android.ground.model.imagery.TileSource
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.DocumentSnapshot
import java.net.URL
import org.junit.Test
import org.mockito.Mockito.*

class SurveyConverterTest {
  @Test
  fun toSurvey_whenPresent_convertsTileSources() {
    val testUrl = "http://test123"
    val doc = SurveyDocument(tileSources = listOf(TileSourceNestedObject(url = testUrl)))
    val snapshot = createSurveyDocumentSnapshot("123", doc)

    assertThat(SurveyConverter.toSurvey(snapshot).tileSources)
      .containsExactly(TileSource(URL(testUrl), TileSource.Type.MOG_COLLECTION))
  }

  @Test
  fun toSurvey_whenMissing_skipsTileSources() {
    val doc = SurveyDocument()
    val snapshot = createSurveyDocumentSnapshot("123", doc)

    assertThat(SurveyConverter.toSurvey(snapshot).tileSources).isEmpty()
  }

  @Test
  fun toSurvey_whenUrlMissing_skipsTileSources() {
    val doc = SurveyDocument(tileSources = listOf(TileSourceNestedObject(url = null)))
    val snapshot = createSurveyDocumentSnapshot("123", doc)

    assertThat(SurveyConverter.toSurvey(snapshot).tileSources).isEmpty()
  }

  private fun createSurveyDocumentSnapshot(
    id: String,
    surveyDocument: SurveyDocument
  ): DocumentSnapshot {
    val snapshot = mock(DocumentSnapshot::class.java)
    `when`(snapshot.id).thenReturn(id)
    `when`(snapshot.toObject(SurveyDocument::class.java)).thenReturn(surveyDocument)
    return snapshot
  }
}
