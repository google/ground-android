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

package com.google.android.gnd.service.firestore;

import static com.google.android.gnd.util.Enums.toEnum;
import static com.google.android.gnd.util.Localization.getLocalizedMessage;
import static com.google.android.gnd.util.Streams.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Form.Field;
import com.google.android.gnd.vo.Form.Field.Type;
import com.google.android.gnd.vo.Form.MultipleChoice;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java8.util.Optional;

@IgnoreExtraProperties
public class FormDoc {
  public Map<String, String> titles = new HashMap<>();

  public List<Element> elements;

  public @ServerTimestamp Date serverTimeCreated;

  public @ServerTimestamp Date serverTimeModified;

  public Form toObject(String formId) {
    return Form.newBuilder()
        .setId(formId)
        .setTitle(getLocalizedMessage(titles))
        .setElements(stream(elements).map(Element::toObject).collect(toImmutableList()))
        .build();
  }

  @IgnoreExtraProperties
  public static class Element {
    public String id;

    public String type;

    public String cardinality;

    // TODO: labels or label?
    public Map<String, String> labels;

    public List<Option> options;

    public boolean required;

    static Form.Element toObject(Element em) {
      return toField(em).map(Form.Element::ofField).orElse(Form.Element.ofUnknown());
    }

    private static Optional<Field> toField(Element em) {
      Form.Field.Builder field = Form.Field.newBuilder();
      switch (toEnum(Field.Type.class, em.type)) {
        case TEXT:
          field.setType(Type.TEXT);
          break;
        case MULTIPLE_CHOICE:
          field.setType(Type.MULTIPLE_CHOICE);
          field.setMultipleChoice(toMultipleChoice(em));
          break;
        default:
          return Optional.empty();
      }
      field.setRequired(em.required);
      field.setId(em.id);
      field.setLabel(getLocalizedMessage(em.labels));
      return Optional.of(field.build());
    }

    private static MultipleChoice toMultipleChoice(Element em) {
      MultipleChoice.Builder mc = MultipleChoice.newBuilder();
      mc.setCardinality(toEnum(MultipleChoice.Cardinality.class, em.cardinality));
      if (em.options != null) {
        mc.setOptions(stream(em.options).map(Option::toOption).collect(toImmutableList()));
      }
      return mc.build();
    }

    public static class Option {
      public String code;
      public Map<String, String> labels;

      public static MultipleChoice.Option toOption(Option option) {
        MultipleChoice.Option.Builder builder = MultipleChoice.Option.newBuilder();
        if (option.code != null) {
          builder.setCode(option.code);
        }
        if (option.labels != null) {
          builder.setLabel(getLocalizedMessage(option.labels));
        }
        return builder.build();
      }
    }
  }
}
