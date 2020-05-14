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

package com.google.android.gnd.ui.editobservation;

import androidx.databinding.ViewDataBinding;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.ui.common.ViewModelFactory;

/** Generates {@link ViewDataBinding} instance for a given {@link Field}. */
public final class FieldViewBindingFactory {

  private FieldViewBindingFactory() {}

  public static ViewDataBinding create(Field field, ViewModelFactory viewModelFactory) {
    switch (field.getType()) {
      case TEXT:
        break;
      case MULTIPLE_CHOICE:
        break;
      case PHOTO:
        break;
      default:
        throw new IllegalArgumentException("Unsupported field type: " + field.getType());
    }
    return null;
  }
}
