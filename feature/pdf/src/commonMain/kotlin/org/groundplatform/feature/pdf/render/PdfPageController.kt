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
 *
 * @param coverPages Pages drawn ahead of the body by someone else, e.g. the QR page. They take the
 *   document's first page numbers, so the body is numbered from [coverPages] + 1, but they are not
 *   counted by [pageCount] and so stay out of the numbering the footer prints.
 */
internal class PdfPageController(
  private val cursor: PdfCursor,
  private val lifecycle: PageLifecycle,
  private val coverPages: Int = 0,
) {
  interface PageLifecycle {
    /** Called after a new page has been allocated. The header should be drawn here. */
    fun onPageStarted(pageNumber: Int)

    /** Called before the page is closed. The footer and per-page flush should happen here. */
    fun onPageEnding(pageNumber: Int)
  }

  private var pageIndex = 0
  private var pageOpen = false

  var isFirstTableRowOnPage = true
    private set

  /**
   * Number of body pages emitted so far, which is the numbering the footer prints. Equals the
   * current body page number while a page is open.
   */
  val pageCount: Int
    get() = pageIndex

  /** Position of the current page within the document, cover pages included. */
  private val documentPageNumber: Int
    get() = coverPages + pageIndex

  fun ensurePage() {
    if (!pageOpen) beginPage()
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
    lifecycle.onPageEnding(documentPageNumber)
    pageOpen = false
  }

  private fun beginPage() {
    pageIndex++
    pageOpen = true
    isFirstTableRowOnPage = true
    cursor.reset()
    lifecycle.onPageStarted(documentPageNumber)
  }
}
