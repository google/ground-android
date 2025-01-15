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
import com.google.android.ground.proto.Survey as SurveyProto
import com.google.android.ground.proto.Survey
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.collections.immutable.toPersistentMap

/** Converts between Firestore documents and [SurveyModel] instances. */
internal object SurveyConverter {

  @Throws(DataStoreException::class)
  fun toSurvey(doc: DocumentSnapshot, jobs: List<Job> = listOf()): SurveyModel {
    if (!doc.exists()) throw DataStoreException("Missing survey")

    val surveyFromProto = parseSurveyFromDocument(doc)
    val jobMap = convertJobsToMap(jobs)
    val dataSharingTerms = getDataSharingTerms(surveyFromProto)

    return createSurveyModel(doc, surveyFromProto, jobMap, dataSharingTerms)
  }

  /** Parse survey data from the DocumentSnapshot. */
  private fun parseSurveyFromDocument(doc: DocumentSnapshot): SurveyProto =
    SurveyProto::class.parseFrom(doc, 1)

  /** Convert a list of jobs into a map for easy lookup. */
  private fun convertJobsToMap(jobs: List<Job>): Map<String, Job> = jobs.associateBy { it.id }

  /** Extract dataSharingTerms from survey. */
  private fun getDataSharingTerms(surveyProto: SurveyProto): Survey.DataSharingTerms? =
    if (surveyProto.dataSharingTerms.type == Survey.DataSharingTerms.Type.TYPE_UNSPECIFIED) {
      null
    } else {
      surveyProto.dataSharingTerms
    }

  /** Build SurveyModel from parsed data. */
  private fun createSurveyModel(
    doc: DocumentSnapshot,
    surveyProto: SurveyProto,
    jobMap: Map<String, Job>,
    dataSharingTerms: Survey.DataSharingTerms?,
  ): SurveyModel =
    SurveyModel(
      id = surveyProto.id.ifEmpty { doc.id },
      title = surveyProto.name,
      description = surveyProto.description,
      jobMap = jobMap.toPersistentMap(),
      acl = surveyProto.aclMap.entries.associate { it.key to it.value.toString() },
      dataSharingTerms = dataSharingTerms,
    )
}
