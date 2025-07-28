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
package org.groundplatform.android.ui.common

import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.databinding.BindingAdapter
import com.google.android.gms.common.SignInButton
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.groundplatform.android.R
import org.groundplatform.android.ui.util.loadBitmapWithCorrectOrientation
import timber.log.Timber

/**
 * Container for adapter methods defining custom data binding behavior. This class cannot be made
 * injectable, since binding adapters must be static.
 */
object BindingAdapters {

  @JvmStatic
  @BindingAdapter("onClick")
  fun bindGoogleSignOnButtonClick(button: SignInButton, onClickCallback: View.OnClickListener?) {
    button.setOnClickListener(onClickCallback)
  }

  @JvmStatic
  @BindingAdapter("imageUrl")
  fun bindUri(view: ImageView?, url: String?) {
    if (view == null || url.isNullOrEmpty()) return

    CoroutineScope(Dispatchers.IO).launch {
      try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()
        val inputStream = connection.inputStream
        val bitmap = BitmapFactory.decodeStream(inputStream)

        withContext(Dispatchers.Main) { view.setImageBitmap(bitmap) }
      } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) { view.setImageResource(R.drawable.outline_error_outline_24) }
      }
    }
  }

  @JvmStatic
  @BindingAdapter("imageUri")
  fun bindUri(view: ImageView?, uri: Uri?) {
    if (view == null || uri == null) return

    try {
      val rotatedBitmap = loadBitmapWithCorrectOrientation(view.context, uri)
      view.setImageBitmap(rotatedBitmap)
    } catch (e: IOException) {
      Timber.e(e, "Failed to load image: IO error")
      setErrorImage(view)
    } catch (e: IllegalArgumentException) {
      Timber.e(e, "Failed to load image: Invalid arguments")
      setErrorImage(view)
    }
  }

  @JvmStatic
  @BindingAdapter("tint")
  fun bindImageTint(imageView: ImageView, colorId: Int) {
    if (colorId == 0) {
      // Workaround for default value from uninitialized LiveData.
      return
    }
    val tint = ContextCompat.getColor(imageView.context, colorId)
    ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(tint))
  }

  @JvmStatic
  @BindingAdapter("visible")
  fun bindVisible(view: View, visible: Boolean) {
    view.visibility = if (visible) View.VISIBLE else View.GONE
  }

  private fun setErrorImage(view: ImageView) {
    view.setImageResource(R.drawable.outline_error_outline_24)
  }
}
