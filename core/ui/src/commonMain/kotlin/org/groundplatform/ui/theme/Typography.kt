package org.groundplatform.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ground_android.core.ui.generated.resources.Res
import ground_android.core.ui.generated.resources.manrope_bold
import ground_android.core.ui.generated.resources.manrope_medium
import ground_android.core.ui.generated.resources.urbanist_bold
import ground_android.core.ui.generated.resources.urbanist_medium

@Composable
private fun manropeFontFamily() =
  FontFamily(
    org.jetbrains.compose.resources.Font(
      resource = Res.font.manrope_medium,
      weight = FontWeight.Medium,
      style = FontStyle.Normal,
    ),
    org.jetbrains.compose.resources.Font(
      resource = Res.font.manrope_bold,
      weight = FontWeight.Bold,
      style = FontStyle.Normal,
    ),
  )

@Composable
private fun urbanistFontFamily() =
  FontFamily(
    org.jetbrains.compose.resources.Font(
      resource = Res.font.urbanist_medium,
      weight = FontWeight.Medium,
      style = FontStyle.Normal,
    ),
    org.jetbrains.compose.resources.Font(
      resource = Res.font.urbanist_bold,
      weight = FontWeight.Bold,
      style = FontStyle.Normal,
    ),
  )

@Composable
fun appTypography() =
  Typography(
    displayLarge =
      TextStyle(
        fontFamily = urbanistFontFamily(),
        fontWeight = FontWeight.Medium,
        fontSize = 57.sp,
        lineHeight = 64.sp,
      ),
    displayMedium =
      TextStyle(
        fontFamily = urbanistFontFamily(),
        fontWeight = FontWeight.Medium,
        fontSize = 45.sp,
        lineHeight = 52.sp,
      ),
    displaySmall =
      TextStyle(
        fontFamily = urbanistFontFamily(),
        fontWeight = FontWeight.Medium,
        fontSize = 36.sp,
        lineHeight = 44.sp,
      ),
    headlineLarge =
      TextStyle(
        fontFamily = manropeFontFamily(),
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 40.sp,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = manropeFontFamily(),
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 36.sp,
      ),
    headlineSmall =
      TextStyle(
        fontFamily = manropeFontFamily(),
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = manropeFontFamily(),
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = manropeFontFamily(),
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
      ),
    titleSmall =
      TextStyle(
        fontFamily = manropeFontFamily(),
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = manropeFontFamily(),
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = manropeFontFamily(),
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = manropeFontFamily(),
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = manropeFontFamily(),
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = manropeFontFamily(),
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
      ),
    bodySmall =
      TextStyle(
        fontFamily = manropeFontFamily(),
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
      ),
  )
