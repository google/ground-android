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
import com.google.android.gnd.persistence.local.room.entity.FormEntity;
import com.google.android.gnd.persistence.local.room.entity.LayerEntity;
import java.util.List;

/**
 * Represents relationship between LayerEntity and FormEntity.
 *
 * <p>Querying any of the below data class automatically loads the field annotated as @Relation.
 */
public class LayerEntityAndRelations {
  @Embedded public LayerEntity layerEntity;

  @Relation(parentColumn = "id", entityColumn = "layer_id", entity = FormEntity.class)
  public List<FormEntityAndRelations> formEntityAndRelations;
}
