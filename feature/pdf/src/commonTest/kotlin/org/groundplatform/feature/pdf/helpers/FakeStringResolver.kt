package org.groundplatform.feature.pdf.helpers

import org.groundplatform.ui.util.StringResolver
import org.jetbrains.compose.resources.StringResource

/**
 * [org.groundplatform.ui.util.StringResolver] for tests so display logic can be asserted without a
 * Compose resource runtime.
 */
object FakeStringResolver : StringResolver {

  override suspend fun resolve(resource: StringResource): String = resource.key

  override suspend fun resolve(resource: StringResource, vararg formatArgs: Any): String =
    "${resource.key}(${formatArgs.joinToString()})"
}
