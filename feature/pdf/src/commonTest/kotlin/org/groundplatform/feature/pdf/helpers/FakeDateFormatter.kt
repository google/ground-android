package org.groundplatform.feature.pdf.helpers

import org.groundplatform.ui.util.DateFormatter

/** [DateFormatter] for tests, so assertions don't depend on the host locale or time zone. */
object FakeDateFormatter : DateFormatter {

  override fun formatDate(millis: Long): String = "DATE($millis)"

  override fun formatTime(millis: Long): String = "TIME($millis)"
}
