package com.google.android.ground.ui.common

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.google.android.ground.R

val provider =
  GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
  )

val googleSansFontName = GoogleFont("Google Sans")

val googleSansFontFamily =
  FontFamily(Font(googleFont = googleSansFontName, fontProvider = provider))
