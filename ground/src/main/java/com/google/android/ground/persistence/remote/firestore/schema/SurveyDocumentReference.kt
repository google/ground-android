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

package com.google.android.ground.persistence.remote.firestore.schema

import com.google.android.ground.model.Survey
import com.google.android.ground.persistence.remote.firestore.base.FluentDocumentReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import durdinapps.rxfirebase2.RxFirestore
import io.reactivex.Maybe

private const val LOIS = "lois"
private const val SUBMISSIONS = "submissions"

class SurveyDocumentReference internal constructor(ref: DocumentReference) :
    FluentDocumentReference(ref) {

    fun lois(): LoiCollectionReference {
        return LoiCollectionReference(reference().collection(LOIS))
    }

    fun submissions(): SubmissionCollectionReference {
        return SubmissionCollectionReference(reference().collection(SUBMISSIONS))
    }

    fun get(): Maybe<Survey> {
        return RxFirestore.getDocument(reference())
            .map { doc: DocumentSnapshot -> SurveyConverter.toSurvey(doc) }
    }
}
