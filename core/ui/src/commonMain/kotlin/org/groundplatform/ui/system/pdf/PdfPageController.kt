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
package org.groundplatform.ui.system.pdf

/**
 * Platform-agnostic page state machine for PDF rendering. Delegates the actual page allocation and
 * drawing to a platform-specific [PageLifecycle] implementation.
 */
internal class PdfPageController(
  private val cursor: PdfCursor,
  private val lifecycle: PageLifecycle,
) {
  interface PageLifecycle {
    /** Called after a new page has been allocated. The header should be drawn here. */
    fun onPageStarted(pageNumber: Int)

    /** Called before the page is closed. The footer and per-page flush should happen here. */
    fun onPageEnding(pageNumber: Int)
  }

  private var pageIndex = 0
  private var pageOpen = false

  /** Number of pages emitted so far. Equals the current page number while a page is open. */
  val pageCount: Int
    get() = pageIndex

  fun ensurePage() {
    if (!pageOpen) beginPage()
  }

  fun newPageIfShort(spaceNeeded: Float) {
    ensurePage()
    if (cursor.fits(spaceNeeded)) return
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
    pageOpen = true
    cursor.reset()
    lifecycle.onPageStarted(pageIndex)
  }
}
