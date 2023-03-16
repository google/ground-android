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
package com.google.android.ground.ui.datacollection.components

/** Defines a unique action that can be bound to a [TaskButton], along with the UI styling. */
enum class ButtonAction(val type: Type, val theme: Theme) {
  // All tasks
  CONTINUE(Type.TEXT, Theme.DARK_GREEN),
  SKIP(Type.TEXT, Theme.LIGHT_GREEN),
  UNDO(Type.ICON, Theme.LIGHT_GREEN),

  // Drop a pin task
  DROP_PIN(Type.TEXT, Theme.OUTLINED),

  // Draw a polygon task
  ADD_PIN(Type.TEXT_ICON, Theme.OUTLINED),
  COMPLETE(Type.TEXT, Theme.OUTLINED);

  enum class Type {
    TEXT,
    ICON,
    TEXT_ICON,
  }

  enum class Theme {
    DARK_GREEN,
    LIGHT_GREEN,
    OUTLINED,
  }
}
