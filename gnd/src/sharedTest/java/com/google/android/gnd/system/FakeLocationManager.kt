/*
 * Copyright 2022 Google LLC
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

package com.google.android.gnd.system

import android.location.Location
import com.google.android.gnd.rx.BooleanOrError
import io.reactivex.Flowable
import io.reactivex.Single
import javax.inject.Inject

class FakeLocationManager @Inject constructor() : LocationManager {

    override fun getLocationUpdates(): Flowable<Location> {
        TODO("Not yet implemented")
    }

    override fun enableLocationUpdates(): Single<BooleanOrError> {
        return Single.just(BooleanOrError.trueValue())
    }

    override fun disableLocationUpdates(): Single<BooleanOrError> {
        return Single.just(BooleanOrError.falseValue())
    }
}