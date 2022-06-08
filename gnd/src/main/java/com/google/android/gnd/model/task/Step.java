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

package com.google.android.gnd.model.task;

import com.google.auto.value.AutoOneOf;

/**
 * Represents an step of a task. The only valid type of task step is currently {@code FIELD};
 * this abstraction exists for future use (e.g., to support headings, subtasks).
 */
@AutoOneOf(Step.Type.class)
public abstract class Step {

  public enum Type {
    UNKNOWN,
    FIELD
  }

  public int getIndex() {
    switch (getType()) {
      case FIELD:
        return getField().getIndex();
      case UNKNOWN:
        // Intentional fall-through.
      default:
        // Fall back for unknown / bad index.
        return hashCode();
    }
  }

  public abstract Type getType();

  public abstract Field getField();

  public abstract Object getUnknown();

  public static Step ofField(Field field) {
    return AutoOneOf_Step.field(field);
  }

  public static Step ofUnknown() {
    return AutoOneOf_Step.unknown(new Object());
  }
}
