package org.groundplatform.ui.components.qrcode

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
  Box(modifier = Modifier.fillMaxSize()) {
    GroundQrCode(
      modifier = Modifier.align(Alignment.Center),
      content = "https://www.google.com",
      contentDescription = "Google",
      centerLogoPainter = null,
    )
  }
}