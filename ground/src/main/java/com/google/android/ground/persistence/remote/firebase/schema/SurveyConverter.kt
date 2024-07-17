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

import com.google.android.ground.model.Survey as SurveyModel
import com.google.android.ground.model.job.Job
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.android.ground.persistence.remote.firebase.protobuf.parseFrom
import com.google.android.ground.persistence.remote.firebase.schema.JobConverter.toJob
import com.google.android.ground.proto.Survey as SurveyProto
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.collections.immutable.toPersistentMap

/** Converts between Firestore documents and [SurveyModel] instances. */
internal object SurveyConverter {

  @Throws(DataStoreException::class)
  fun toSurvey(doc: DocumentSnapshot, jobs: List<Job> = listOf()): SurveyModel {
    if (!doc.exists()) throw DataStoreException("Missing survey")

    val surveyFromProto = SurveyProto::class.parseFrom(doc, 1)
    val surveyFromDocument = doc.toObject(SurveyDocument::class.java)

    val jobMap =
      if (surveyFromDocument?.jobs == null) {
        jobs.associateBy { it.id }
      } else {
        surveyFromDocument.jobs.entries.associate { it.key to toJob(it.key, it.value) }
      }

    return SurveyModel(
      surveyFromProto.id.ifEmpty { doc.id },
      surveyFromProto.name.ifEmpty { surveyFromDocument?.title.orEmpty() },
      surveyFromProto.description.ifEmpty { surveyFromDocument?.description.orEmpty() },
      jobMap.toPersistentMap(),
      surveyFromProto.aclMap
        .ifEmpty { surveyFromDocument?.acl ?: mapOf() }
        .entries
        .associate { it.key to it.value.toString() },
    )
  }
}
