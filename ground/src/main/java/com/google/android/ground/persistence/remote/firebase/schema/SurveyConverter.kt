/*
 * Copyright 2020 Google LLC
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

import com.google.android.ground.model.Survey
import com.google.android.ground.model.imagery.TileSource
import com.google.android.ground.model.job.Job
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.android.ground.persistence.remote.firebase.copyInto
import com.google.android.ground.persistence.remote.firebase.schema.JobConverter.toJob
import com.google.android.ground.proto.survey
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import timber.log.Timber

/** Converts between Firestore documents and [Survey] instances. */
internal object SurveyConverter {

  @Throws(DataStoreException::class)
  fun toSurvey(doc: DocumentSnapshot): Survey {

    Timber.e("!!! Survey: " + doc.copyInto(survey {}))

    if (!doc.exists()) throw DataStoreException("Missing survey")

    val pd =
      DataStoreException.checkNotNull(doc.toObject(SurveyDocument::class.java), "surveyDocument")

    val jobMap = mutableMapOf<String, Job>()
    if (pd.jobs != null) {
      pd.jobs.forEach { (id: String, obj: JobNestedObject) -> jobMap[id] = toJob(id, obj) }
    }

    val tileSources = mutableListOf<TileSource>()
    if (pd.tileSources != null) {
      convertTileSources(pd, tileSources)
    }

    return Survey(
      doc.id,
      pd.title.orEmpty(),
      pd.description.orEmpty(),
      jobMap.toPersistentMap(),
      tileSources.toPersistentList(),
      pd.acl ?: mapOf()
    )
  }

  private fun convertTileSources(pd: SurveyDocument, builder: MutableList<TileSource>) {
    pd.tileSources
      ?.mapNotNull { it.url }
      ?.forEach { url -> builder.add(TileSource(url, TileSource.Type.MOG_COLLECTION)) }
  }
}
