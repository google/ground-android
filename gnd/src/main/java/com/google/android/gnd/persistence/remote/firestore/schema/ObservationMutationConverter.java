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

import com.google.android.gnd.model.User;
import com.google.android.gnd.model.observation.DateResponse;
import com.google.android.gnd.model.observation.MultipleChoiceResponse;
import com.google.android.gnd.model.observation.NumberResponse;
import com.google.android.gnd.model.observation.ObservationMutation;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.model.observation.ResponseDelta;
import com.google.android.gnd.model.observation.TextResponse;
import com.google.android.gnd.model.observation.TimeResponse;
import com.google.android.gnd.persistence.remote.DataStoreException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.firestore.FieldValue;
import java.util.Map;
import timber.log.Timber;

/**
 * Converts between Firestore maps used to merge updates and {@link ObservationMutation} instances.
 */
class ObservationMutationConverter {
  static final String FEATURE_ID = "featureId";
  private static final String LAYER_ID = "layerId";
  private static final String FORM_ID = "formId";
  private static final String RESPONSES = "responses";
  private static final String CREATED = "created";
  private static final String LAST_MODIFIED = "lastModified";

  static ImmutableMap<String, Object> toMap(ObservationMutation mutation, User user)
      throws DataStoreException {
    ImmutableMap.Builder<String, Object> map = ImmutableMap.builder();
    AuditInfoNestedObject auditInfo = AuditInfoConverter.fromMutationAndUser(mutation, user);
    switch (mutation.getType()) {
      case CREATE:
        map.put(CREATED, auditInfo);
        map.put(LAST_MODIFIED, auditInfo);
        break;
      case UPDATE:
        map.put(LAST_MODIFIED, auditInfo);
        break;
      case DELETE:
        // TODO.
      case UNKNOWN:
      default:
        throw new DataStoreException("Unsupported mutation type: " + mutation.getType());
    }
    map.put(FEATURE_ID, mutation.getFeatureId())
        .put(LAYER_ID, mutation.getLayerId())
        .put(FORM_ID, mutation.getForm().getId())
        .put(RESPONSES, toMap(mutation.getResponseDeltas()));
    return map.build();
  }

  private static Map<String, Object> toMap(ImmutableList<ResponseDelta> responseDeltas) {
    ImmutableMap.Builder<String, Object> map = ImmutableMap.builder();
    for (ResponseDelta delta : responseDeltas) {
      map.put(
          delta.getFieldId(),
          delta
              .getNewResponse()
              .map(ObservationMutationConverter::toObject)
              .orElse(FieldValue.delete()));
    }
    return map.build();
  }

  private static Object toObject(Response response) {
    if (response instanceof TextResponse) {
      return ((TextResponse) response).getText();
    } else if (response instanceof MultipleChoiceResponse) {
      return ((MultipleChoiceResponse) response).getSelectedOptionIds();
    } else if (response instanceof NumberResponse) {
      return ((NumberResponse) response).getValue();
    } else if (response instanceof TimeResponse) {
      return ((TimeResponse) response).getTime();
    } else if (response instanceof DateResponse) {
      return ((DateResponse) response).getDate();
    } else {
      Timber.e("Unknown response type: %s", response.getClass().getName());
      return null;
    }
  }
}
