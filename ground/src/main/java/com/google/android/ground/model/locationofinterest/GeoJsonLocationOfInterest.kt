/*
 * Copyright 2021 Google LLC
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
package com.google.android.ground.model.locationofinterest

import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.Survey
import com.google.android.ground.model.job.Job
import com.google.common.collect.ImmutableList
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

/** User-defined map LOI consisting of a set of geometries defined in GeoJson format.  */
data class GeoJsonLocationOfInterest(
    override val id: String,
    override val survey: Survey,
    override val job: Job,
    override val customId: String?,
    override val caption: String?,
    override val created: AuditInfo,
    override val lastModified: AuditInfo,
    val geoJsonString: String
) :
    LocationOfInterest() {

    // TODO: Parse at conversion type instead of here.
    val geoJson: JSONObject
        get() =// TODO: Parse at conversion type instead of here.
            try {
                JSONObject(geoJsonString)
            } catch (e: JSONException) {
                Timber.d("Invalid GeoJSON in LOI %s", id)
                JSONObject()
            }
    val captionFromProperties: String
        get() = findProperty(CAPTION_PROPERTIES)
    val idFromProperties: String
        get() = findProperty(ID_PROPERTIES)

    private fun findProperty(matchKeys: ImmutableList<String>): String {
        val properties = geoJson.optJSONObject(PROPERTIES_KEY) ?: return ""

        for (matchKey in matchKeys) {
            val keyIterator = properties.keys()

            while (keyIterator.hasNext()) {
                val key = keyIterator.next()
                if (key.equals(matchKey, ignoreCase = true)) {
                    return properties.opt(key)?.toString() ?: ""
                }
            }
        }

        return ""
    }

    // TODO: Remove once all callers are converted to Kotlin. We only retain this for Java interop.
    class Builder : LocationOfInterest.Builder() {
        var geoJsonString: String = ""
            @JvmSynthetic set

        // Preserve typing.
        override fun setId(value: String): Builder = apply { super.setId(value) }
        override fun setCaption(value: String?): Builder = apply { super.setCaption(value) }
        override fun setCreated(value: AuditInfo): Builder = apply { super.setCreated(value) }
        override fun setCustomId(value: String?): Builder = apply { super.setCustomId(value) }
        override fun setJob(value: Job): Builder = apply { super.setJob(value) }
        override fun setSurvey(value: Survey): Builder = apply { super.setSurvey(value) }
        override fun setLastModified(value: AuditInfo): Builder =
            apply { super.setLastModified(value) }

        fun setGeoJsonString(value: String): Builder = apply { this.geoJsonString = value }
        fun build(): GeoJsonLocationOfInterest {
            val survey = survey ?: throw Exception("Expected a survey")
            val job = job ?: throw Exception("Expected a job")
            val created = created ?: throw Exception("Expected a creation timestamp")
            val lastModified = lastModified ?: throw Exception("Expected a last modified timestamp")

            return GeoJsonLocationOfInterest(
                id,
                survey,
                job,
                customId,
                caption,
                created,
                lastModified,
                geoJsonString
            )
        }
    }

    override fun toBuilder(): Builder =
        Builder().setGeoJsonString(geoJsonString).setCaption(caption).setJob(job)
            .setCreated(created).setId(id)
            .setCustomId(customId).setSurvey(survey).setLastModified(lastModified)

    companion object {
        private val CAPTION_PROPERTIES = ImmutableList.of("caption", "label", "name")
        private val ID_PROPERTIES = ImmutableList.of("id", "identifier", "id_prod")
        private const val PROPERTIES_KEY = "properties"

        @JvmStatic
        fun newBuilder(): Builder = Builder()
    }
}