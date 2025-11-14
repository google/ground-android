package org.groundplatform.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AppSizes(
  val jobActionButtonSize: Dp = 80.dp,
  val jobActionIconSize: Dp = 28.dp
)

internal val LocalSizes = compositionLocalOf { AppSizes() }

val MaterialTheme.sizes: AppSizes
  @Composable
  @ReadOnlyComposable
  get() = LocalSizes.current
