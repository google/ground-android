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
package org.groundplatform.android.persistence.remote

import android.net.Uri
import java.io.File
import javax.inject.Inject

class FakeRemoteStorageManager @Inject internal constructor() : RemoteStorageManager {
  override suspend fun getDownloadUrl(remoteDestinationPath: String): Uri = Uri.EMPTY

  override suspend fun uploadMediaFromFile(file: File, remoteDestinationPath: String) = Unit
}
