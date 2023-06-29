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
package com.google.android.ground.ui.signin

import com.google.android.ground.repository.UserRepository
import com.google.android.ground.ui.common.AbstractViewModel
import javax.inject.Inject

class SignInViewModel @Inject internal constructor(private val userRepository: UserRepository) :
  AbstractViewModel() {
  fun onSignInButtonClick() {
    userRepository.signIn()
  }
}
