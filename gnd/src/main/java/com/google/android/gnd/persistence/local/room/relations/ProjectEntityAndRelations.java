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

package com.google.android.gnd.persistence.local.room.relations;

import androidx.room.Embedded;
import androidx.room.Relation;
import com.google.android.gnd.persistence.local.room.entity.BaseMapEntity;
import com.google.android.gnd.persistence.local.room.entity.LayerEntity;
import com.google.android.gnd.persistence.local.room.entity.ProjectEntity;
import java.util.List;

/**
 * Represents relationship between ProjectEntity and LayerEntity.
 *
 * <p>Querying any of the below data class automatically loads the field annotated as @Relation.
 */
public class ProjectEntityAndRelations {

  @Embedded public ProjectEntity projectEntity;

  @Relation(parentColumn = "id", entityColumn = "project_id", entity = LayerEntity.class)
  public List<LayerEntityAndRelations> layerEntityAndRelations;

  @Relation(
      parentColumn = "id",
      entityColumn = "project_id",
      entity = BaseMapEntity.class)
  public List<BaseMapEntity> baseMapEntityAndRelations;
}
