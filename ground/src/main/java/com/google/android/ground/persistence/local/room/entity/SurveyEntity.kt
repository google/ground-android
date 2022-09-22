/*
 * Copyright 2019 Google LLC
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
package com.google.android.ground.persistence.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.ground.model.Survey
import com.google.android.ground.model.basemap.BaseMap
import com.google.android.ground.model.job.Job
import com.google.android.ground.persistence.local.room.converter.BaseMapConverter
import com.google.android.ground.persistence.local.room.entity.JobEntity.Companion.toJob
import com.google.android.ground.persistence.local.room.relations.SurveyEntityAndRelations
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import java.net.MalformedURLException
import org.json.JSONObject
import timber.log.Timber

@Entity(tableName = "survey")
data class SurveyEntity(
  @ColumnInfo(name = "id") @PrimaryKey val id: String,
  @ColumnInfo(name = "title") val title: String?,
  @ColumnInfo(name = "description") val description: String?,
  @ColumnInfo(name = "acl") val acl: JSONObject?
) {

  companion object {
    fun fromSurvey(survey: Survey): SurveyEntity {
      return SurveyEntity(
        id = survey.id,
        title = survey.title,
        description = survey.description,
        acl = JSONObject(survey.acl as Map<*, *>?)
      )
    }

    fun toSurvey(surveyEntityAndRelations: SurveyEntityAndRelations): Survey {
      val jobMap = ImmutableMap.builder<String, Job>()
      val baseMaps = ImmutableList.builder<BaseMap>()
      for (jobEntityAndRelations in surveyEntityAndRelations.jobEntityAndRelations) {
        val job = toJob(jobEntityAndRelations!!)
        jobMap.put(job.id, job)
      }
      for (source in surveyEntityAndRelations.baseMapEntityAndRelations) {
        try {
          baseMaps.add(BaseMapConverter.convertFromDataStoreObject(source))
        } catch (e: MalformedURLException) {
          Timber.d("Skipping basemap source with malformed URL %s", source.url)
        }
      }
      val surveyEntity = surveyEntityAndRelations.surveyEntity
      return Survey(
        surveyEntity.id,
        surveyEntity.title!!,
        surveyEntity.description!!,
        jobMap.build(),
        baseMaps.build(),
        toStringMap(surveyEntity.acl)
      )
    }

    private fun toStringMap(jsonObject: JSONObject?): ImmutableMap<String, String> {
      val builder: ImmutableMap.Builder<String, String> = ImmutableMap.builder()
      val keys = jsonObject!!.keys()
      while (keys.hasNext()) {
        val key = keys.next()
        val value = jsonObject.optString(key, null.toString())
        builder.put(key, value)
      }
      return builder.build()
    }
  }
}
