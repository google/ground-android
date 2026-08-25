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
package org.groundplatform.feature.pdf.render

internal sealed interface MapOverlay {
  /** A filled polygon, such as the north arrow's triangle. */
  data class Polygon(val points: List<PdfOffset>) : MapOverlay

  /** A stroked line, such as the scale bar. */
  data class Line(val line: PdfLine) : MapOverlay

  /** [text] laid out centered within [boxWidth] points, its top-left corner at [offset]. */
  data class Text(val text: String, val boxWidth: Int, val offset: PdfOffset) : MapOverlay
}
