/*
 * Copyright 2020 Google LLC
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
package com.google.android.ground.persistence.remote

class TransferProgress private constructor(
    val state: UploadState,
    val byteCount: Int = 0,
    val bytesTransferred: Int = 0
) {

    enum class UploadState {
        STARTING, IN_PROGRESS, PAUSED, FAILED, COMPLETED
    }

    companion object {
        private val STARTING = TransferProgress(UploadState.STARTING)
        private val PAUSED = TransferProgress(UploadState.PAUSED)
        private val FAILED = TransferProgress(UploadState.FAILED)
        private val COMPLETED = TransferProgress(UploadState.COMPLETED)

        fun starting(): TransferProgress {
            return STARTING
        }

        @JvmStatic
        fun inProgress(byteCount: Int, bytesTransferred: Int): TransferProgress {
            return TransferProgress(UploadState.IN_PROGRESS, byteCount, bytesTransferred)
        }

        @JvmStatic
        fun paused(): TransferProgress {
            return PAUSED
        }

        fun failed(): TransferProgress {
            return FAILED
        }

        fun completed(): TransferProgress {
            return COMPLETED
        }
    }
}