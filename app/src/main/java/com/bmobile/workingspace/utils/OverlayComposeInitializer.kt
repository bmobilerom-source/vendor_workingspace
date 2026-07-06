/*
 * Copyright (C) 2022 The Android Open Source Project
 * Copyright (C) 2026 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmobile.workingspace.utils

import android.view.View
import android.view.ViewTreeObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Enables Compose outside of an Activity — e.g. a [ComposeView][androidx.compose.ui.platform.ComposeView]
 * added directly via [android.view.WindowManager.addView], such as the game bar, quick panel,
 * call ringer overlay, and keymap editor overlay.
 *
 * Without a [LifecycleOwner] on the root view, attaching a ComposeView to such a window throws
 * `IllegalStateException: ViewTreeLifecycleOwner not found` from
 * `WindowRecomposer_androidKt.createLifecycleAwareWindowRecomposer` — this is what crashed the
 * app whenever the game bar/session overlay was shown.
 *
 * Mirrors `com.android.systemui.compose.ComposeInitializer`, which uses the same approach for
 * SystemUI's own non-Activity overlay windows (e.g. Dynamic Island).
 *
 * Usage:
 * ```
 * wm.addView(composeView, layoutParams)
 * OverlayComposeInitializer.onAttachedToWindow(composeView)
 * // ...
 * OverlayComposeInitializer.onDetachedFromWindow(composeView)
 * wm.removeViewImmediate(composeView)
 * ```
 */
object OverlayComposeInitializer {

    fun onAttachedToWindow(root: View) {
        if (root.findViewTreeLifecycleOwner() != null) {
            return
        }

        val lifecycleOwner = OverlayViewLifecycleOwner(root)

        val savedStateRegistryOwner = object : SavedStateRegistryOwner {
            private val controller = SavedStateRegistryController.create(this).apply {
                performRestore(null)
            }

            override val savedStateRegistry get() = controller.savedStateRegistry
            override val lifecycle: Lifecycle get() = lifecycleOwner.lifecycle
        }

        lifecycleOwner.onCreate()

        root.setViewTreeLifecycleOwner(lifecycleOwner)
        root.setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
    }

    fun onDetachedFromWindow(root: View) {
        (root.findViewTreeLifecycleOwner() as? OverlayViewLifecycleOwner)?.onDestroy()
        root.setViewTreeLifecycleOwner(null)
        root.setViewTreeSavedStateRegistryOwner(null)
    }
}

/**
 * [LifecycleOwner] for a window-root [View] not backed by an Activity/Fragment. Tracks window
 * visibility/focus so Compose can pause recomposition while the overlay is hidden:
 * not visible -> CREATED, visible but unfocused -> STARTED, visible and focused -> RESUMED.
 *
 * Overlay windows here use FLAG_NOT_FOCUSABLE, so in practice this settles between CREATED and
 * STARTED, which is sufficient for Compose to render and recompose.
 */
private class OverlayViewLifecycleOwner(private val view: View) : LifecycleOwner {
    private val registry = LifecycleRegistry(this)

    private val windowVisibleListener =
        ViewTreeObserver.OnWindowVisibilityChangeListener { updateState() }
    private val windowFocusListener =
        ViewTreeObserver.OnWindowFocusChangeListener { updateState() }

    override val lifecycle: Lifecycle get() = registry

    fun onCreate() {
        registry.currentState = Lifecycle.State.CREATED
        view.viewTreeObserver.addOnWindowVisibilityChangeListener(windowVisibleListener)
        view.viewTreeObserver.addOnWindowFocusChangeListener(windowFocusListener)
        updateState()
    }

    fun onDestroy() {
        view.viewTreeObserver.removeOnWindowVisibilityChangeListener(windowVisibleListener)
        view.viewTreeObserver.removeOnWindowFocusChangeListener(windowFocusListener)
        registry.currentState = Lifecycle.State.DESTROYED
    }

    private fun updateState() {
        registry.currentState = when {
            view.windowVisibility != View.VISIBLE -> Lifecycle.State.CREATED
            !view.hasWindowFocus() -> Lifecycle.State.STARTED
            else -> Lifecycle.State.RESUMED
        }
    }
}
