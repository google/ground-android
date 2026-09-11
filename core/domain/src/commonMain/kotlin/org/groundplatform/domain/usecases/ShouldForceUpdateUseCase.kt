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
package org.groundplatform.domain.usecases

import org.groundplatform.domain.repository.AppConfigRepositoryInterface

class ShouldForceUpdateUseCase(
  private val appConfigRepository: AppConfigRepositoryInterface,
  private val currentVersion: String,
) {

  /** Returns true if this build must be updated before it may be used. */
  operator fun invoke(): Boolean {
    val appConfig = appConfigRepository.getAppConfig()
    val minRequired = appConfig.minAppVersion
    val forceUpdate = appConfig.forceUpdate

    return forceUpdate && minRequired.isNotBlank() && isOlderVersion(currentVersion, minRequired)
  }

  private fun isOlderVersion(current: String, minRequired: String): Boolean {
    fun String.toSegments() = split('.').map { it.toIntOrNull() ?: 0 }

    val currentParts = current.toSegments()
    val requiredParts = minRequired.toSegments()
    val maxLength = maxOf(currentParts.size, requiredParts.size)

    for (i in 0 until maxLength) {
      val curr = currentParts.getOrElse(i) { 0 }
      val req = requiredParts.getOrElse(i) { 0 }
      if (curr != req) return curr < req
    }

    return false
  }
}
