package com.bmobile.workingspace.utils

import android.view.View
import androidx.compose.ui.platform.ComposeView

fun ComposeView.runOnAttach(block: () -> Unit) {
    if (isAttachedToWindow) {
        block()
        return
    }
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            block()
            removeOnAttachStateChangeListener(this)
        }

        override fun onViewDetachedFromWindow(v: View) {
        }
    })
}
