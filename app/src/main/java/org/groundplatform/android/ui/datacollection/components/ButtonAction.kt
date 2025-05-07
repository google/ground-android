/*
 * Copyright 2023 Google LLC
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
package org.groundplatform.android.ui.datacollection.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.groundplatform.android.R

/** Defines a unique action that can be bound to a [TaskButton], along with the UI styling. */
enum class ButtonAction(
  val theme: Theme,
  @StringRes val textId: Int? = null,
  @DrawableRes val drawableId: Int? = null,
  @StringRes val contentDescription: Int? = null,
) {

  // All tasks
  DONE(Theme.DARK_GREEN, textId = R.string.done),
  NEXT(Theme.DARK_GREEN, textId = R.string.next),
  PREVIOUS(Theme.TRANSPARENT, textId = R.string.previous),
  SKIP(Theme.TRANSPARENT, textId = R.string.skip),
  UNDO(
    Theme.TRANSPARENT,
    drawableId = R.drawable.ic_undo_black,
    contentDescription = R.string.undo,
  ),
  REDO(
    Theme.TRANSPARENT,
    drawableId = R.drawable.ic_redo_black,
    contentDescription = R.string.redo,
  ),

  // Drop a pin task
  DROP_PIN(Theme.OUTLINED, textId = R.string.drop_pin),

  // Draw a polygon task
  ADD_POINT(Theme.OUTLINED, textId = R.string.add_point),
  COMPLETE(Theme.LIGHT_GREEN, textId = R.string.complete_polygon),

  // Capture location task
  CAPTURE_LOCATION(Theme.OUTLINED, textId = R.string.capture);

  enum class Theme {
    DARK_GREEN,
    LIGHT_GREEN,
    OUTLINED,
    TRANSPARENT,
  }
}
