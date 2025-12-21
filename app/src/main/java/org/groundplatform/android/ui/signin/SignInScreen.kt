/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.ui.signin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.common.SignInButton
import org.groundplatform.android.R

@Composable
fun SignInScreen(onSignInClick: () -> Unit) {
  Box(modifier = Modifier.fillMaxSize()) {
    // Background Image
    Image(
      painter = painterResource(id = R.drawable.splash_background),
      contentDescription = "Background",
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Crop
    )
    // Overlay
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(color = Color(0x66146C2E)) // 40% alpha on #146C2E
    )

    // Gradient
    Column(Modifier.fillMaxSize()) {
      Spacer(Modifier.weight(1f))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .background(
            brush = Brush.verticalGradient(
              colors = listOf(Color(0x00006E2C), Color(0xFF003917))
            )
          )
      )
    }

    // Content
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.weight(1f))
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Image(
          painter = painterResource(id = R.drawable.ground_logo),
          contentDescription = "Logo",
          modifier = Modifier
            .width(120.dp)
            .height(120.dp)
        )
        Text(
          text = stringResource(id = R.string.app_name),
          color = Color.White,
          fontSize = 60.sp,
          fontFamily = FontFamily(Font(R.font.display_500)),
          letterSpacing = 0.6.sp // 0.01 * 60sp
        )
      }

      Spacer(modifier = Modifier.weight(1f))

      AndroidView(
        factory = { context ->
          SignInButton(context).apply {
            setSize(SignInButton.SIZE_WIDE)
            setOnClickListener { onSignInClick() }
          }
        },
        modifier = Modifier.wrapContentSize(),
      )

      Spacer(modifier = Modifier.weight(1f))
    }
  }
}

@Preview(showBackground = true)
@Composable
fun SignInScreenPreview() {
  SignInScreen(onSignInClick = {})
}
