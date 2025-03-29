package com.example.memo_app

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ScrollView

class OneWayScrollView(context: Context, attrs: AttributeSet?) : ScrollView(context, attrs) {

    private var previousY: Float = 0f // Храним предыдущее положение пальца

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                // Сохраняем начальную позицию пальца
                previousY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaY = ev.y - previousY // Разница между текущим и предыдущим положением
                if (deltaY > 0 && scrollY == 0) {
                    // Если пытаемся прокрутить вверх при scrollY == 0, блокируем событие
                    return false
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        // Блокируем прокрутку вверх (на уровне touch-событий)
        if (ev.action == MotionEvent.ACTION_MOVE) {
            val deltaY = ev.y - previousY
            if (deltaY > 0 && scrollY == 0) {
                // Блокируем только если пытаемся прокрутить вверх и уже на самом верху
                return false
            }
        }
        return super.onTouchEvent(ev)
    }
}