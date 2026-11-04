package com.team3.dorawing.util

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView

// 아래에 내용이 더 있을 때만 아래쪽 끝을 흐리게 한다 (위쪽은 흐리게 하지 않음)
class FadeScrollView(context: Context, attrs: AttributeSet?) : ScrollView(context, attrs) {
    override fun getTopFadingEdgeStrength(): Float = 0f
}
