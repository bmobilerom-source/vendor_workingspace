/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.bmobile.workingspace.onboarding

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bmobile.workingspace.R
import com.bmobile.workingspace.settings.SettingsActivity
import com.github.appintro.AppIntro2
import com.github.appintro.AppIntroFragment

class WorkingSpaceIntroActivity : AppIntro2() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isSkipButtonEnabled = true
        isColorTransitionsEnabled = true
        setIndicatorColor(
            ContextCompat.getColor(this, R.color.workingspace_intro_accent),
            ContextCompat.getColor(this, R.color.workingspace_intro_accent_muted),
        )
        setBarColor(ContextCompat.getColor(this, R.color.workingspace_intro_bar))
        setNextArrowColor(Color.WHITE)
        setSkipArrowColor(Color.WHITE)
        addSlide(
            AppIntroFragment.createInstance(
                title = getString(R.string.intro_slide_welcome_title),
                description = getString(R.string.intro_slide_welcome_body),
                imageDrawable = R.drawable.intro_slide_welcome,
                backgroundColorRes = R.color.workingspace_intro_bg_1,
                titleColorRes = R.color.workingspace_intro_on_bg,
                descriptionColorRes = R.color.workingspace_intro_on_bg_muted,
            ),
        )
        addSlide(
            AppIntroFragment.createInstance(
                title = getString(R.string.intro_slide_apps_title),
                description = getString(R.string.intro_slide_apps_body),
                imageDrawable = R.drawable.intro_slide_apps,
                backgroundColorRes = R.color.workingspace_intro_bg_2,
                titleColorRes = R.color.workingspace_intro_on_bg,
                descriptionColorRes = R.color.workingspace_intro_on_bg_muted,
            ),
        )
        addSlide(
            AppIntroFragment.createInstance(
                title = getString(R.string.intro_slide_focus_title),
                description = getString(R.string.intro_slide_focus_body),
                imageDrawable = R.drawable.intro_slide_focus,
                backgroundColorRes = R.color.workingspace_intro_bg_3,
                titleColorRes = R.color.workingspace_intro_on_bg,
                descriptionColorRes = R.color.workingspace_intro_on_bg_muted,
            ),
        )
        addSlide(
            AppIntroFragment.createInstance(
                title = getString(R.string.intro_slide_overlay_title),
                description = getString(R.string.intro_slide_overlay_body),
                imageDrawable = R.drawable.intro_slide_overlay,
                backgroundColorRes = R.color.workingspace_intro_bg_4,
                titleColorRes = R.color.workingspace_intro_on_bg,
                descriptionColorRes = R.color.workingspace_intro_on_bg_muted,
            ),
        )
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        finishIntro()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        finishIntro()
    }

    private fun finishIntro() {
        IntroPreferences.setCompleted(this)
        startActivity(Intent(this, SettingsActivity::class.java))
        finish()
    }
}
