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

package com.google.android.gnd.model.observation;

import static java8.lang.Iterables.forEach;

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.Map;
import java8.util.Optional;

/** An immutable map of field ids to related user responses. */
public class ResponseMap {

  /**
   * A map from field id to user response. This map is mutable and therefore should never be exposed
   * outside this class.
   */
  private Map<String, Response> responses;

  /** Private constructor. Use {@link Builder} to obtain an instance. */
  private ResponseMap(Map<String, Response> responses) {
    this.responses = responses;
  }

  /**
   * Returns the user response for the given field id, or empty if the user did not specify a
   * response.
   */
  public Optional<Response> getResponse(String fieldId) {
    return Optional.ofNullable(responses.get(fieldId));
  }

  /** Returns an Iterable over the field ids in this map. */
  public Iterable<String> fieldIds() {
    return responses.keySet();
  }

  public ResponseMap.Builder toBuilder() {
    return builder().putAllResponses(responses);
  }

  /** Returns a builder for constructing and populating new {@link ResponseMap} instances. */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final Map<String, Response> map = new HashMap<>();

    /** Sets or overwrites the response with the specified field id. */
    public Builder putResponse(String fieldId, Response response) {
      map.put(fieldId, response);
      return this;
    }

    public Builder putAllResponses(Map<String, Response> responses) {
      map.putAll(responses);
      return this;
    }

    /** Removes the response with the specified field id. */
    public Builder removeResponse(String fieldId) {
      map.remove(fieldId);
      return this;
    }

    /** Adds, replaces, and/or removes responses based on the provided list of deltas. */
    public Builder applyDeltas(ImmutableList<ResponseDelta> responseDeltas) {
      forEach(responseDeltas, this::applyDelta);
      return this;
    }

    /** Adds, replaces, or removes a responses based on the provided delta. */
    public Builder applyDelta(ResponseDelta responseDelta) {
      responseDelta
          .getNewResponse()
          .ifPresentOrElse(
              newResponse -> responseDelta.getFieldId(),
              () -> removeResponse(responseDelta.getFieldId()));
      return this;
    }

    /** Returns a new immutable instance of {@link ResponseMap}. */
    public ResponseMap build() {
      return new ResponseMap(map);
    }
  }

  @Override
  public String toString() {
    return responses.toString();
  }
}
