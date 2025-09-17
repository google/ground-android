/*
 * Copyright 2024 Google LLC
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
package org.groundplatform.android.ui.components

import android.text.Annotation
import androidx.annotation.StringRes
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.core.text.toSpannable

@Composable
fun HyperlinkText(
  modifier: Modifier = Modifier,
  @StringRes fullTextResId: Int,
  hyperLinks: Map<String, () -> Unit>,
  textStyle: TextStyle = TextStyle.Default,
  linkTextColor: Color = Color.Blue,
  linkTextFontWeight: FontWeight = FontWeight.Normal,
  linkTextDecoration: TextDecoration = TextDecoration.None,
  fontSize: TextUnit = TextUnit.Unspecified,
) {
  val fullText = LocalContext.current.getText(fullTextResId).toSpannable()
  val annotations = fullText.getSpans(0, fullText.length, Annotation::class.java)

  val annotatedString = buildAnnotatedString {
    append(fullText)
    hyperLinks.keys.forEach { action ->
      annotations
        ?.find { it.key == "type" && it.value == action }
        ?.let {
          addStyle(
            style =
              SpanStyle(
                color = linkTextColor,
                fontSize = fontSize,
                fontWeight = linkTextFontWeight,
                textDecoration = linkTextDecoration,
              ),
            start = fullText.getSpanStart(it),
            end = fullText.getSpanEnd(it),
          )
          addStringAnnotation(
            tag = "URL",
            annotation = it.value,
            start = fullText.getSpanStart(it),
            end = fullText.getSpanEnd(it),
          )
        }
      addStyle(style = SpanStyle(fontSize = fontSize), start = 0, end = fullText.length)
    }
  }

  ClickableText(
    modifier = modifier,
    text = annotatedString,
    style = textStyle,
    onClick = {
      annotatedString.getStringAnnotations("URL", it, it).firstOrNull()?.let { stringAnnotation ->
        hyperLinks[stringAnnotation.item]?.let { callback -> callback() }
      }
    },
  )
}
