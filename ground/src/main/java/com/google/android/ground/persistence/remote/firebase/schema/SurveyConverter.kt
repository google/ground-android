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

package com.google.android.ground.persistence.remote.firebase.schema

import com.google.android.ground.model.Survey as SurveyModel
import com.google.android.ground.model.job.Job
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.android.ground.persistence.remote.firebase.protobuf.parseFrom
import com.google.android.ground.proto.Survey as SurveyProto
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.collections.immutable.toPersistentMap

/** Converts between Firestore documents and [SurveyModel] instances. */
internal object SurveyConverter {

  @Throws(DataStoreException::class)
  fun toSurvey(doc: DocumentSnapshot, jobs: List<Job> = listOf()): SurveyModel {
    if (!doc.exists()) throw DataStoreException("Missing survey")

    val surveyFromProto = SurveyProto::class.parseFrom(doc, 1)
    val jobMap = jobs.associateBy { it.id }
    return SurveyModel(
      surveyFromProto.id.ifEmpty { doc.id },
      surveyFromProto.name,
      surveyFromProto.description,
      jobMap.toPersistentMap(),
      surveyFromProto.aclMap.entries.associate { it.key to it.value.toString() },
      DataSharingConsent(
        type = DataSharingConsent.DataSharingConsentType.CUSTOM,
        customText =
          """
          # Introduction

          Ground values your privacy and is committed to protecting your personal information. This form explains how we may collect, use, and share your data for research or other purposes. By signing this form, you consent to the practices described below.

          ## What Data We Collect

          We may collect the following types of data:

          *   Personal Information: Name, contact details, demographic information (if applicable).
          *   Research Data: Responses to surveys, interviews, or other study-related data.
          *   Usage Data: Information about how you interact with our services or website (if applicable).

          ## How We Use Your Data

          We may use your data for the following purposes:

          *   Research: To analyze and publish findings, contribute to scientific knowledge, and improve our services.
          *   Internal Analysis: To understand how our services are used and to make improvements.
          *   Communication: To contact you with updates, information about research results, or opportunities to participate in future studies.

          ## How We Share Your Data

          We may share your data with:

          *   Researchers: We may share de-identified data with qualified researchers for approved studies.
          *   Partners: We may share de-identified data with partner organizations for research or analysis.
          *   Service Providers: We may share your data with trusted third-party service providers who help us deliver our services (e.g., data storage, analysis).

          ## Your Rights

          You have the right to:

          *   Access Your Data: Request a copy of the personal data we hold about you.
          *   Correct Your Data: Ask us to correct any inaccurate or incomplete data.
          *   Withdraw Consent: You may withdraw your consent to data sharing at any time.
          *   Object to Processing: You can object to certain types of processing (e.g., direct marketing).

          ## Data Security

          We take appropriate technical and organizational measures to protect your data from unauthorized access, disclosure, alteration, or destruction.

          ## Data Retention

          We will retain your data for as long as necessary to fulfill the purposes outlined in this form or as required by law.

          ## Changes to this Form

          We may update this form from time to time. We will notify you of any material changes.

          ## Contact Us

          If you have any questions or concerns about our data practices, please contact us at [email address removed].

          ## Consent

          By agreeing below, I acknowledge that I have read and understood this data sharing consent form. I freely give my consent for Ground to collect, use, and share my data as described above.
        """,
      ),
    )
  }
}
