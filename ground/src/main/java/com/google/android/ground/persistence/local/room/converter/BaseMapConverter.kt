/*
 * Copyright 2022 Google LLC
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
package com.google.android.ground.persistence.local.room.converter

import com.google.android.ground.model.basemap.BaseMap
import com.google.android.ground.persistence.local.LocalDataStoreConverter
import com.google.android.ground.persistence.local.room.entity.BaseMapEntity
import java.net.MalformedURLException
import java.net.URL

class BaseMapConverter(val surveyId: String) : LocalDataStoreConverter<BaseMap, BaseMapEntity> {
  companion object {
    @Throws(MalformedURLException::class)
    fun convertFromDataStoreObject(source: BaseMapEntity): BaseMap {
      return BaseMap(URL(source.url), entityToModelType(source))
    }

    private fun entityToModelType(source: BaseMapEntity): BaseMap.BaseMapType {
      return when (source.type) {
        BaseMapEntity.BaseMapEntityType.IMAGE -> BaseMap.BaseMapType.TILED_WEB_MAP
        BaseMapEntity.BaseMapEntityType.GEOJSON -> BaseMap.BaseMapType.MBTILES_FOOTPRINTS
        else -> BaseMap.BaseMapType.UNKNOWN
      }
    }
  }

  override fun convertFromDataStoreObject(entity: BaseMapEntity): BaseMap? =
    Companion.convertFromDataStoreObject(entity)

  override fun convertToDataStoreObject(source: BaseMap): BaseMapEntity =
    BaseMapEntity(
      surveyId = surveyId,
      url = source.url.toString(),
      type = modelToEntityType(source)
    )

  private fun modelToEntityType(source: BaseMap): BaseMapEntity.BaseMapEntityType {
    return when (source.type) {
      BaseMap.BaseMapType.TILED_WEB_MAP -> BaseMapEntity.BaseMapEntityType.IMAGE
      BaseMap.BaseMapType.MBTILES_FOOTPRINTS -> BaseMapEntity.BaseMapEntityType.GEOJSON
      else -> BaseMapEntity.BaseMapEntityType.UNKNOWN
    }
  }
}
