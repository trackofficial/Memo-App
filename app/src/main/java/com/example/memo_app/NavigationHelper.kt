package com.example.memo_app

import android.content.Context
import android.widget.FrameLayout
import android.widget.ImageButton

object NavigationHelper {

    private var currentTabId: Int = -1

    fun updateNavigationSelection(
        context: Context,
        containerFrames: List<FrameLayout>,
        iconButtons: List<ImageButton>,
        selectedContainer: FrameLayout,
        selectedIcon: ImageButton,
        baseIconName: String
    ) {
        if (currentTabId == selectedIcon.id) return
        currentTabId = selectedIcon.id

        containerFrames.forEachIndexed { index, container ->
            val icon = iconButtons[index]
            val isActive = container == selectedContainer

            val targetResId = if (isActive)
                context.resources.getIdentifier("${baseIconName}_pl", "drawable", context.packageName)
            else
                getDefaultIconId(context, icon.id)
            if (!isActive) {
                icon.setBackgroundResource(targetResId)
                icon.alpha = 0.5f
                icon.scaleX = 1f
                icon.scaleY = 1f
                container.setBackgroundResource(android.R.color.transparent)
                return@forEachIndexed
            }
            container.animate()
                .alpha(0.8f)
                .setDuration(160)
                .withStartAction {
                    container.setBackgroundResource(R.drawable.form_for_button_simpleblock)
                }
                .start()

            icon.animate()
                .alpha(1f)
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(100)
                .withEndAction {
                    icon.setBackgroundResource(targetResId)
                    icon.animate()
                        .scaleX(0.8f)
                        .scaleY(0.8f)
                        .setDuration(140)
                        .start()
                }
                .start()
        }
    }

    private fun getDefaultIconId(context: Context, id: Int): Int {
        return when (id) {
            R.id.main_button -> R.drawable.main_button
            R.id.statistic_button -> R.drawable.calendar_button
            R.id.focus_button -> R.drawable.focus_button
            else -> R.drawable.main_button
        }
    }
}