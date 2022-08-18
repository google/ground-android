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

package com.google.android.ground.persistence.remote.firestore.schema

import com.google.android.ground.model.Survey
import com.google.android.ground.model.basemap.BaseMap
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.android.ground.persistence.remote.firestore.schema.JobConverter.toJob
import com.google.common.collect.ImmutableMap
import com.google.firebase.firestore.DocumentSnapshot
import java8.util.Maps
import timber.log.Timber
import java.net.MalformedURLException
import java.net.URL

/** Converts between Firestore documents and [Survey] instances.  */
internal object SurveyConverter {

    @Throws(DataStoreException::class)
    fun toSurvey(doc: DocumentSnapshot): Survey {
        val pd = DataStoreException.checkNotNull(
            doc.toObject(SurveyDocument::class.java), "surveyDocument"
        )
        val survey = Survey.newBuilder()
        survey.setId(doc.id).setTitle(pd.title.orEmpty()).setDescription(pd.description.orEmpty())
        if (pd.jobs != null) {
            Maps.forEach(pd.jobs) { id: String, obj: JobNestedObject ->
                survey.putJob(toJob(id, obj))
            }
        }
        survey.setAcl(ImmutableMap.copyOf(pd.acl))
        if (pd.baseMaps != null) {
            convertOfflineBaseMapSources(pd, survey)
        }
        return survey.build()
    }

    private fun convertOfflineBaseMapSources(pd: SurveyDocument, builder: Survey.Builder) {
        for ((url) in pd.baseMaps!!) {
            if (url == null) {
                Timber.d("Skipping base map source in survey with missing URL")
                continue
            }
            try {
                builder.addBaseMap(
                    BaseMap(URL(url), BaseMap.typeFromExtension(url))
                )
            } catch (e: MalformedURLException) {
                Timber.d("Skipping base map source in survey with malformed URL")
            }
        }
    }
}