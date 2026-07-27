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

/**
 * Platform-agnostic page state machine for PDF rendering. Delegates the actual page allocation and
 * drawing to a platform-specific [PageLifecycle] implementation.
 */
internal class PdfPageController(
  private val cursor: PdfCursor,
  private val lifecycle: PageLifecycle,
) {
  interface PageLifecycle {
    /** Called after a new content page has been allocated. The header should be drawn here. */
    fun onPageStarted(pageNumber: Int)

    /**
     * Called before the content page is closed. The footer and per-page flush should happen here.
     */
    fun onPageEnding(pageNumber: Int)
  }

  private var pageIndex = 0
  private var contentPageIndex = 0
  private var pageOpen = false

  var isFirstTableRowOnPage = true
    private set

  /**
   * Number of pages emitted so far, standalone pages included. Equals the current page number while
   * a page is open.
   */
  val pageCount: Int
    get() = pageIndex

  /**
   * Number of content pages emitted so far. Standalone pages are excluded, so this is the number
   * shown in the footer of the page currently open.
   */
  val contentPageCount: Int
    get() = contentPageIndex

  fun ensurePage() {
    if (!pageOpen) beginPage()
  }

  /**
   * Emits a page that carries no header or footer and is left out of the content page numbering.
   * Any open content page is closed first so the standalone page keeps its place in the document.
   * [draw] receives the page number and is responsible for opening and closing the page.
   */
  fun standalonePage(draw: (pageNumber: Int) -> Unit) {
    finalizePage()
    pageIndex++
    draw(pageIndex)
  }

  /** Records that the first table row on the current page has been drawn. */
  fun consumeFirstTableRowOnPage() {
    isFirstTableRowOnPage = false
  }

  fun newPageIfShort(spaceNeeded: Float) {
    ensurePage()
    if (cursor.fits(spaceNeeded) || cursor.isAtPageTop) return
    finalizePage()
    beginPage()
  }

  fun finalizePage() {
    if (!pageOpen) return
    lifecycle.onPageEnding(pageIndex)
    pageOpen = false
  }

  private fun beginPage() {
    pageIndex++
    contentPageIndex++
    pageOpen = true
    isFirstTableRowOnPage = true
    cursor.reset()
    lifecycle.onPageStarted(pageIndex)
  }
}
