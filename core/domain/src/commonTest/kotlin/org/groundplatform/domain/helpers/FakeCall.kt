/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.domain.helpers

/**
 * Records calls to a fake method and allows overriding its behavior for testing.
 *
 * @param I the input parameter type
 * @param O the return type
 * @param behavior the initial suspending behavior to execute when invoked
 */
class FakeCall<I, O>(private var behavior: suspend (I) -> O) {
  /** All inputs passed to this call, in invocation order. */
  private val _calls = mutableListOf<I>()
  val calls: List<I>
    get() = _calls

  /** Number of times this method was called. */
  val callCount: Int
    get() = _calls.size

  /** Records the [input] and executes the current [behavior]. */
  suspend operator fun invoke(input: I): O {
    _calls.add(input)
    return behavior(input)
  }

  /** Clears all recorded calls. */
  fun reset() {
    _calls.clear()
  }

  /** Replaces the current behavior with [newBehavior]. */
  fun overrideBehavior(newBehavior: suspend (I) -> O) {
    behavior = newBehavior
  }
}
