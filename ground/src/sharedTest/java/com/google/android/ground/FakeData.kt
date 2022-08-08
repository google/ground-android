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
package com.google.android.ground

import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.Survey
import com.google.android.ground.model.TermsOfService
import com.google.android.ground.model.User
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.locationofinterest.Point
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import org.locationtech.jts.geom.GeometryFactory

/**
 * Shared test data constants. Tests are expected to override existing or set missing values when
 * the specific value is relevant to the test.
 */
object FakeData {
    // TODO: Replace constants with calls to newFoo() methods.
    @JvmField
    val TERMS_OF_SERVICE: TermsOfService = TermsOfService.builder()
        .setId("TERMS_OF_SERVICE")
        .setText("Fake Terms of Service text")
        .build()

    @JvmField
    val JOB = newJob().build()

    @JvmStatic
    fun newJob(): Job.Builder {
        return Job.newBuilder().setId("JOB").setName("Job")
    }

    @JvmField
    val USER =
        User.builder().setId("user_id").setEmail("user@gmail.com").setDisplayName("User").build()

    @JvmField
    val USER_2 =
        User.builder().setId("user_id_2").setEmail("user2@gmail.com").setDisplayName("User2")
            .build()

    @JvmField
    val SURVEY: Survey = newSurvey().build()

    @JvmStatic
    fun newSurvey(): Survey.Builder {
        return Survey.newBuilder()
            .setId("SURVEY")
            .setTitle("Survey title")
            .setDescription("Test survey description")
            .setAcl(ImmutableMap.of(USER.email, "data_collector"))
    }

    @JvmField
    val POINT_OF_INTEREST = LocationOfInterest(
        "loi id",
        SURVEY,
        JOB,
        null,
        null,
        AuditInfo.now(USER),
        AuditInfo.now(USER),
        Point.newBuilder().setLatitude(0.0).setLongitude(0.0).build().toGeometry()
    )

    @JvmField
    val VERTICES: ImmutableList<Point> = ImmutableList.of(
        Point.newBuilder().setLatitude(0.0).setLongitude(0.0).build(),
        Point.newBuilder().setLatitude(10.0).setLongitude(10.0).build(),
        Point.newBuilder().setLatitude(20.0).setLongitude(20.0).build(),
        Point.newBuilder().setLatitude(0.0).setLongitude(0.0).build(),
    )

    @JvmField
    val AREA_OF_INTEREST: LocationOfInterest = LocationOfInterest(
        "loi id",
        SURVEY,
        JOB,
        "",
        "",
        AuditInfo.now(USER),
        AuditInfo.now(USER),
        GeometryFactory().createPolygon(VERTICES.map { it.toGeometry().coordinate }.toTypedArray()),
    )

    @JvmField
    val POINT = Point.newBuilder().setLatitude(42.0).setLongitude(18.0).build()
}