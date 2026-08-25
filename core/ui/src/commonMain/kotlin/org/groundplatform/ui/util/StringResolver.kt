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
package org.groundplatform.ui.util

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * Resolves string resources to localized text.
 *
 * Abstracted behind an interface so display logic can be unit-tested with a fake, without a
 * Compose/Skiko resource runtime on the test classpath.
 */
interface StringResolver {

  suspend fun resolve(resource: StringResource): String

  suspend fun resolve(resource: StringResource, vararg formatArgs: Any): String
}

/** [StringResolver] backed by Compose Multiplatform resources. */
object ComposeStringResolver : StringResolver {

  override suspend fun resolve(resource: StringResource): String = getString(resource)

  override suspend fun resolve(resource: StringResource, vararg formatArgs: Any): String =
    getString(resource, *formatArgs)
}
