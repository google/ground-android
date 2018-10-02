/*
 * Copyright 2018 Google LLC
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

package com.google.android.gnd.vo;

import static java8.util.stream.StreamSupport.stream;

import com.google.auto.value.AutoOneOf;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java8.util.Optional;
import javax.annotation.Nullable;

@AutoValue
public abstract class Form {
  @Nullable
  public abstract String getId();

  @Nullable
  public abstract String getTitle();

  public abstract ImmutableList<Element> getElements();

  public Optional<Field> getField(String id) {
    return stream(getElements())
        .map(Element::getField)
        .filter(f -> f != null && f.getId().equals(id))
        .findFirst();
  }

  public static Builder newBuilder() {
    return new AutoValue_Form.Builder().setElements(ImmutableList.of());
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(@Nullable String newId);

    public abstract Builder setTitle(@Nullable String newTitle);

    public abstract Builder setElements(ImmutableList<Element> newElementsList);

    public abstract Form build();
  }

  @AutoOneOf(Element.Type.class)
  public abstract static class Element {
    public enum Type {
      UNKNOWN,
      FIELD,
      SUBFORM
    }

    public abstract Type getType();

    @Nullable
    public abstract Field getField();

    @Nullable
    public abstract Form getSubform();

    @Nullable
    public abstract Object getUnknown();

    public static Element ofField(Field field) {
      return AutoOneOf_Form_Element.field(field);
    }

    public static Element ofSubform(Form subform) {
      return AutoOneOf_Form_Element.subform(subform);
    }

    public static Element ofUnknown() {
      return AutoOneOf_Form_Element.unknown(new Object());
    }
  }

  @AutoValue
  public abstract static class Field {
    public enum Type {
      TEXT,
      MULTIPLE_CHOICE
    }

    @Nullable
    public abstract String getId();

    @Nullable
    public abstract Type getType();

    @Nullable
    public abstract String getLabel();

    public abstract boolean isRequired();

    @Nullable
    public abstract MultipleChoice getMultipleChoice();

    public static Builder newBuilder() {
      return new AutoValue_Form_Field.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setId(@Nullable String newId);

      public abstract Builder setType(@Nullable Type newType);

      public abstract Builder setLabel(@Nullable String newLabel);

      public abstract Builder setRequired(boolean newRequired);

      public abstract Builder setMultipleChoice(@Nullable MultipleChoice multipleChoice);

      public abstract Field build();
    }
  }

  @AutoValue
  public abstract static class MultipleChoice {
    public enum Cardinality {
      SELECT_ONE,
      SELECT_MULTIPLE
    }

    public abstract ImmutableList<Option> getOptions();

    public Optional<Option> getOption(String code) {
      return stream(getOptions()).filter(o -> o.getCode().equals(code)).findFirst();
    }

    @Nullable
    public abstract Cardinality getCardinality();

    public Optional<Integer> getIndex(String code) {
      for (int i = 0; i < getOptions().size(); i++) {
        if (getOptions().get(i).getCode().equals(code)) {
          return Optional.of(i);
        }
      }
      return Optional.empty();
    }

    public static Builder newBuilder() {
      return new AutoValue_Form_MultipleChoice.Builder().setOptions(ImmutableList.of());
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setOptions(ImmutableList<Option> newOptions);

      public abstract ImmutableList.Builder<Option> optionsBuilder();

      public abstract Builder setCardinality(@Nullable Cardinality newCardinality);

      public abstract MultipleChoice build();
    }

    @AutoValue
    public abstract static class Option {
      @Nullable
      public abstract String getCode();

      @Nullable
      public abstract String getLabel();

      public static Builder newBuilder() {
        return new AutoValue_Form_MultipleChoice_Option.Builder();
      }

      @AutoValue.Builder
      public abstract static class Builder {
        public abstract Builder setCode(@Nullable String newCode);

        public abstract Builder setLabel(@Nullable String newLabel);

        public abstract Option build();
      }
    }
  }
}
