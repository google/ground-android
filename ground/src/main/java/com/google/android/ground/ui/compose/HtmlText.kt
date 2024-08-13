package com.google.android.ground.ui.compose

import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.google.android.ground.R

@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier) {
  AndroidView(
    modifier = modifier,
    factory = { context -> TextView(context) },
    update = {
      it.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
      it.movementMethod = ScrollingMovementMethod()
      it.setBackgroundResource(R.color.md_theme_onPrimary)
      it.setPadding(24, 16, 24, 16)
    },
  )
}
