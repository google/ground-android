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
package com.google.android.ground.persistence.local.room.entity

import androidx.room.*
import com.google.android.ground.model.basemap.BaseMap
import com.google.android.ground.model.basemap.BaseMap.BaseMapType
import java.net.MalformedURLException
import java.net.URL

@Entity(
  tableName = "offline_base_map_source",
  foreignKeys =
    [
      ForeignKey(
        entity = SurveyEntity::class,
        parentColumns = ["id"],
        childColumns = ["survey_id"],
        onDelete = ForeignKey.CASCADE
      )],
  indices = [Index("survey_id")]
)
data class BaseMapEntity(
  @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) val id: Int? = 0,
  @ColumnInfo(name = "survey_id") val surveyId: String,
  @ColumnInfo(name = "url") val url: String,
  @ColumnInfo(name = "type") val type: BaseMapEntityType
) {

  enum class BaseMapEntityType {
    GEOJSON,
    IMAGE,
    UNKNOWN
  }

  companion object {
    @JvmStatic
    @Throws(MalformedURLException::class)
    fun toModel(source: BaseMapEntity): BaseMap {
      return BaseMap(URL(source.url), entityToModelType(source))
    }

    private fun modelToEntityType(source: BaseMap): BaseMapEntityType {
      return when (source.type) {
        BaseMapType.TILED_WEB_MAP -> BaseMapEntityType.IMAGE
        BaseMapType.MBTILES_FOOTPRINTS -> BaseMapEntityType.GEOJSON
        else -> BaseMapEntityType.UNKNOWN
      }
    }

    private fun entityToModelType(source: BaseMapEntity): BaseMapType {
      return when (source.type) {
        BaseMapEntityType.IMAGE -> BaseMapType.TILED_WEB_MAP
        BaseMapEntityType.GEOJSON -> BaseMapType.MBTILES_FOOTPRINTS
        else -> BaseMapType.UNKNOWN
      }
    }

    fun fromModel(surveyId: String, source: BaseMap): BaseMapEntity =
      BaseMapEntity(
        surveyId = surveyId,
        url = source.url.toString(),
        type = modelToEntityType(source)
      )
  }
}
