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
import com.google.android.ground.model.basemap.BaseMap
import com.google.android.ground.model.job.Job
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.android.ground.persistence.remote.firebase.schema.JobConverter.toJob
import com.google.firebase.firestore.DocumentSnapshot
import java.net.MalformedURLException
import java.net.URL
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import timber.log.Timber

/** Converts between Firestore documents and [Survey] instances. */
internal object SurveyConverter {

  @Throws(DataStoreException::class)
  fun toSurvey(doc: DocumentSnapshot): Survey {
    val pd =
      DataStoreException.checkNotNull(doc.toObject(SurveyDocument::class.java), "surveyDocument")

    val jobMap = mutableMapOf<String, Job>()
    if (pd.jobs != null) {
      pd.jobs.forEach { (id: String, obj: JobNestedObject) -> jobMap[id] = toJob(id, obj) }
    }

    val baseMaps = mutableListOf<BaseMap>()
    if (pd.baseMaps != null) {
      convertOfflineBaseMapSources(pd, baseMaps)
    }

    return Survey(
      doc.id,
      pd.title.orEmpty(),
      pd.description.orEmpty(),
      jobMap.toPersistentMap(),
      baseMaps.toPersistentList(),
      pd.acl ?: mapOf()
    )
  }

  private fun convertOfflineBaseMapSources(pd: SurveyDocument, builder: MutableList<BaseMap>) {
    for ((url) in pd.baseMaps!!) {
      if (url == null) {
        Timber.d("Skipping base map source in survey with missing URL")
        continue
      }
      try {
        Timber.d("Converting base map source: $url")
        builder.add(BaseMap(URL(url), BaseMap.typeFromExtension(url)))
      } catch (e: MalformedURLException) {
        Timber.d("Skipping base map source in survey with malformed URL")
      }
    }
  }
}
