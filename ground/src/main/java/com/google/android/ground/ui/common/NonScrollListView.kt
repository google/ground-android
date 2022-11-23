package com.google.android.ground.ui.common

import android.content.Context
import android.util.AttributeSet
import android.widget.ListView

class NonScrollListView @JvmOverloads constructor(
  context: Context, attrs: AttributeSet? = null
) : ListView(context, attrs) {
  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val customHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
      Int.MAX_VALUE shr 2, MeasureSpec.AT_MOST
    )
    super.onMeasure(widthMeasureSpec, customHeightMeasureSpec)
    val params = layoutParams
    params.height = measuredHeight
  }
}