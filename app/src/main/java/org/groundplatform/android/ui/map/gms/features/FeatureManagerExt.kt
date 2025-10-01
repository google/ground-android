/*
 * Copyright 2025 Google LLC
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

package org.groundplatform.android.ui.map.gms.features

import org.groundplatform.android.model.geometry.LineString
import org.groundplatform.android.ui.map.Feature

/**
 * Returns true if this [Feature] represents a user-drawn "draft" line string (i.e. in-progress
 * polygon drawing that should be updated in place).
 */
fun Feature.isDraftLineString(): Boolean =
  geometry is LineString && !clusterable && selected && tag.type == Feature.Type.USER_POLYGON
