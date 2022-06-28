/*
 * Copyright 2018 Google LLC
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
package com.google.android.gnd.util

object Localization {
    @JvmStatic
    fun getLocalizedMessage(messages: Map<String, String>): String {
        // TODO(#711): Allow user to select survey/form language and use here.
        // TODO: Return Optional and handle empty strings in client code.
        return messages["_"] ?: messages["en"] ?: messages["pt"] ?: "<Untitled>"
    }
}
