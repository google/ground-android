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
package com.google.android.ground.test

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
    val JOB = Job(name = "Job", id = "JOB")

    @JvmField
    val USER =
        User("user_id", "user@gmail.com", "User")

    @JvmField
    val USER_2 =
        User("user_id_2", "user2@gmail.com", "User2")

    @JvmField
    val SURVEY: Survey = Survey(
        "SURVEY",
        "Survey title",
        "Test survey description",
        ImmutableMap.of(),
        ImmutableList.of(),
        ImmutableMap.of(USER.email, "data_collector"))

    @JvmField
    val POINT_OF_INTEREST = LocationOfInterest(
        "loi id",
        SURVEY,
        JOB,
        null,
        null,
        AuditInfo(USER),
        AuditInfo(USER),
        Point(0.0, 0.0).toGeometry()
    )

    @JvmField
    val VERTICES: ImmutableList<Point> = ImmutableList.of(
        Point(0.0, 0.0),
        Point(10.0, 10.0),
        Point(20.0, 20.0),
        Point(0.0, 0.0),
    )

    @JvmField
    val AREA_OF_INTEREST: LocationOfInterest = LocationOfInterest(
        "loi id",
        SURVEY,
        JOB,
        "",
        "",
        AuditInfo(USER),
        AuditInfo(USER),
        GeometryFactory().createPolygon(VERTICES.map { it.toGeometry().coordinate }.toTypedArray()),
    )

    @JvmField
    val POINT = Point(42.0, 18.0)
}