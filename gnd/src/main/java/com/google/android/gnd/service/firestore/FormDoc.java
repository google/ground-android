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

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.android.gnd.model.Form;
import com.google.android.gnd.model.Form.Field.FieldTypeCase;
import com.google.android.gnd.model.Form.MultipleChoice;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java8.util.Optional;

import static com.google.android.gnd.util.Protos.toEnum;
import static java8.util.stream.Collectors.toList;
import static java8.util.stream.StreamSupport.stream;

@IgnoreExtraProperties
public class FormDoc {
  // TODO: -> title
  public Map<String, String> titles = new HashMap<>();

  public List<Element> elements;

  public @ServerTimestamp Date serverTimeCreated;

  public @ServerTimestamp Date serverTimeModified;

  public static Form toProto(DocumentSnapshot doc) {
    FormDoc f = doc.toObject(FormDoc.class);
    return Form.newBuilder()
        .setId(doc.getId())
        .putAllTitle(f.titles)
        .addAllElements(stream(f.elements).map(Element::toProto).collect(toList()))
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

    static Form.Element toProto(Element em) {
      Form.Element.Builder element = Form.Element.newBuilder();
      element.setId(em.id);
      toField(em).ifPresent(element::setField);
      return element.build();
    }

    private static Optional<Form.Field> toField(Element em) {
      Form.Field.Builder field = Form.Field.newBuilder();
      switch (toEnum(FieldTypeCase.class, em.type)) {
        case TEXT_FIELD:
          field.setTextField(Form.TextField.getDefaultInstance());
          break;
        case MULTIPLE_CHOICE:
          field.setMultipleChoice(toMultipleChoice(em));
          break;
        default:
          return Optional.empty();
      }
      field.setRequired(em.required);
      field.setId(em.id);
      field.putAllLabel(em.labels);
      return Optional.of(field.build());
    }

    private static MultipleChoice toMultipleChoice(Element em) {
      MultipleChoice.Builder mc = MultipleChoice.newBuilder();
      mc.setCardinality(toEnum(MultipleChoice.Cardinality.class, em.cardinality));
      if (em.options != null) {
        stream(em.options).map(Option::toOption).forEach(mc::addOptions);
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
          builder.putAllLabels(option.labels);
        }
        return builder.build();
      }
    }
  }
}
