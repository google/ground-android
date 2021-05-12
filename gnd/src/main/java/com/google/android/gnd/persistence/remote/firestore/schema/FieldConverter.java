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

package com.google.android.gnd.persistence.remote.firestore.schema;

import static com.google.android.gnd.util.Enums.toEnum;
import static com.google.android.gnd.util.Localization.getLocalizedMessage;

import com.google.android.gnd.model.form.Field;
import java8.util.Objects;
import java8.util.Optional;
import timber.log.Timber;

/** Converts between Firestore nested objects and {@link Field} instances. */
class FieldConverter {

  static Optional<Field> toField(String id, ElementNestedObject em) {
    Field.Builder field = Field.newBuilder();
    switch (toEnum(Field.Type.class, em.getType())) {
      case TEXT_FIELD:
        field.setType(Field.Type.TEXT_FIELD);
        break;
      case MULTIPLE_CHOICE:
        field.setType(Field.Type.MULTIPLE_CHOICE);
        field.setMultipleChoice(MultipleChoiceConverter.toMultipleChoice(em));
        break;
      case PHOTO:
        field.setType(Field.Type.PHOTO);
        break;
      case NUMBER:
        field.setType(Field.Type.NUMBER);
        break;
      case LOCATION:
        field.setType(Field.Type.LOCATION);
        break;
      default:
        Timber.d("Unsupported form element type: " + em.getType());
        return Optional.empty();
    }
    field.setRequired(em.getRequired() != null && em.getRequired());
    field.setId(id);
    // Default index to -1 to degrade gracefully on older dev db instances and projects.
    field.setIndex(Objects.requireNonNullElse(em.getIndex(), -1));
    field.setLabel(getLocalizedMessage(em.getLabel()));
    return Optional.of(field.build());
  }
}
