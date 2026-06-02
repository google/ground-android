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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PdfPageControllerTest {

  private val lifecycle =
    object : PdfPageController.PageLifecycle {
      val events: MutableList<PageEvent> = mutableListOf()

      override fun onPageStarted(pageNumber: Int) {
        events += PageEvent.Started(pageNumber)
      }

      override fun onPageEnding(pageNumber: Int) {
        events += PageEvent.Ending(pageNumber)
      }
    }
  private val cursor = PdfCursor()
  private val controller = PdfPageController(cursor, lifecycle)

  @Test
  fun `Should have zero pages at the start`() {
    assertEquals(0, controller.pageCount)
    assertTrue(lifecycle.events.isEmpty())
  }

  @Test
  fun `ensurePage starts the a page`() {
    controller.ensurePage()

    assertEquals(1, controller.pageCount)
    assertEquals(listOf<PageEvent>(PageEvent.Started(1)), lifecycle.events)
  }

  @Test
  fun `ensurePage is idempotent while the page is open`() {
    controller.ensurePage()
    controller.ensurePage()
    controller.ensurePage()

    assertEquals(1, controller.pageCount)
    assertEquals(listOf<PageEvent>(PageEvent.Started(1)), lifecycle.events)
  }

  @Test
  fun `finalizePage does nothing if there is no page open`() {
    controller.finalizePage()

    assertEquals(0, controller.pageCount)
    assertTrue(lifecycle.events.isEmpty())
  }

  @Test
  fun `finalizePage does nothing when the page is already closed`() {
    controller.ensurePage()
    controller.finalizePage()
    controller.finalizePage()

    assertEquals(1, controller.pageCount)
    assertEquals(listOf(PageEvent.Started(1), PageEvent.Ending(1)), lifecycle.events)
  }

  @Test
  fun `ensurePage followed by finalize emits start and end events`() {
    controller.ensurePage()
    controller.finalizePage()

    assertEquals(listOf(PageEvent.Started(1), PageEvent.Ending(1)), lifecycle.events)
  }

  @Test
  fun `newPageIfShort starts a page if there is none open`() {
    controller.newPageIfShort(spaceNeeded = 10f)

    assertEquals(1, controller.pageCount)
    assertEquals(listOf<PageEvent>(PageEvent.Started(1)), lifecycle.events)
  }

  @Test
  fun `newPageIfShort does not emit more pages if impossible to fit content in a new page`() {
    controller.newPageIfShort(spaceNeeded = Float.MAX_VALUE)

    assertEquals(1, controller.pageCount)
    assertEquals(listOf<PageEvent>(PageEvent.Started(1)), lifecycle.events)
  }

  @Test
  fun `newPageIfShort does nothing if the content fits in the current page`() {
    controller.ensurePage()
    lifecycle.events.clear()

    controller.newPageIfShort(spaceNeeded = 10f)

    assertEquals(1, controller.pageCount)
    assertTrue(lifecycle.events.isEmpty())
  }

  @Test
  fun `newPageIfShort starts a new page if the content overflows`() {
    controller.ensurePage()
    cursor.advance(100f)
    lifecycle.events.clear()

    controller.newPageIfShort(spaceNeeded = Float.MAX_VALUE)

    assertEquals(2, controller.pageCount)
    assertEquals(listOf(PageEvent.Ending(1), PageEvent.Started(2)), lifecycle.events)
  }

  @Test
  fun `newPageIfShort should set the cursor at the start of the new page`() {
    controller.ensurePage()
    cursor.advance(500f)

    controller.newPageIfShort(spaceNeeded = Float.MAX_VALUE)

    assertEquals(PdfConfig.MARGIN.toFloat(), cursor.y)
  }

  @Test
  fun `Adding multiple pages emits the correct start and end events`() {
    controller.ensurePage()
    cursor.advance(100f)
    controller.newPageIfShort(spaceNeeded = Float.MAX_VALUE)
    cursor.advance(100f)
    controller.newPageIfShort(spaceNeeded = Float.MAX_VALUE)
    controller.finalizePage()

    assertEquals(3, controller.pageCount)
    assertEquals(
      listOf(
        PageEvent.Started(1),
        PageEvent.Ending(1),
        PageEvent.Started(2),
        PageEvent.Ending(2),
        PageEvent.Started(3),
        PageEvent.Ending(3),
      ),
      lifecycle.events,
    )
  }

  @Test
  fun `pageCount reflects the current page number while the page is open`() {
    controller.ensurePage()
    assertEquals(1, controller.pageCount)

    cursor.advance(100f)
    controller.newPageIfShort(spaceNeeded = Float.MAX_VALUE)
    assertEquals(2, controller.pageCount)
  }

  private sealed interface PageEvent {
    data class Started(val pageNumber: Int) : PageEvent

    data class Ending(val pageNumber: Int) : PageEvent
  }
}
