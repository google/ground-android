/*
 * Copyright 2023 Google LLC
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

package mogtest

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

@Deprecated("delete me")
object IOUtils {
  private var COPY_BUFFER_SIZE = 8192


  @Throws(IOException::class)
  fun streamBytes(stream: InputStream): ByteArray {
    val bytes = ByteArrayOutputStream()
    copyStream(stream, bytes)
    return bytes.toByteArray()
  }

   @Throws(IOException::class)
  fun copyStream(copyFrom: InputStream, copyTo: OutputStream) {
    val buffer = ByteArray(COPY_BUFFER_SIZE)
    var length: Int
    while (copyFrom.read(buffer).also { length = it } > 0) {
      copyTo.write(buffer, 0, length)
    }
    copyTo.flush()
    copyTo.close()
    copyFrom.close()
  }
}