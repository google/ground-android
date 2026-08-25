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
package org.groundplatform.feature.pdf.render.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.groundplatform.feature.pdf.render.image.PdfImageSet.ImageRef

class PdfImageSetTest {

  @Test
  fun `get returns null for a ref that is not in the set`() {
    val set = PdfImageSet(emptyMap())

    assertNull(set[ImageRef.Qr])
    assertNull(set[ImageRef.Photo("missing.jpg")])
  }

  @Test
  fun `release invokes the onRelease callback`() {
    var released = 0
    val set = PdfImageSet(emptyMap(), onRelease = { released++ })

    set.release()

    assertEquals(1, released)
  }

  @Test
  fun `Photo refs are equal when their filenames match`() {
    assertEquals(ImageRef.Photo("a.jpg"), ImageRef.Photo("a.jpg"))
    assertTrue(ImageRef.Photo("a.jpg") != ImageRef.Photo("b.jpg"))
  }
}
