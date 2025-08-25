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
package org.groundplatform.android.data.remote

open class DataStoreException(message: String?) : RuntimeException(message) {
  companion object {
    @JvmStatic
    @Throws(DataStoreException::class)
    fun <T> checkNotNull(reference: T?, field: String): T =
      reference ?: throw DataStoreException("Missing $field")

    /**
     * Checks if the provided object is of the same type as (or a subtype of) the specified type. If
     * not, a `DataStoreException` is thrown with relevant details.
     */
    @JvmStatic
    @Throws(DataStoreException::class)
    fun <T : Any> checkType(expectedType: Class<*>, obj: T): T {
      // TODO: Handle Kotlin Long (java.lang.Long) vs Java primitive long (long)
      // Issue URL: https://github.com/google/ground-android/issues/2743
      if (obj.javaClass == java.lang.Long::class.java && expectedType == Long::class.java) {
        return obj
      }
      if (!expectedType.isAssignableFrom(obj.javaClass)) {
        throw DataStoreException("Expected ${expectedType.name}, got ${obj.javaClass.name}")
      }
      return obj
    }
  }
}
