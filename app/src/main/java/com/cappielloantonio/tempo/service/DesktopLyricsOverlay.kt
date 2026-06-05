package com.cappielloantonio.tempo.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.Preferences

object DesktopLyricsOverlay {
    private const val FONT_SIZE_TINY = 14
    private const val FONT_SIZE_SMALL = 18
    private const val FONT_SIZE_MEDIUM = 24
    private const val FONT_SIZE_LARGE = 32
    private const val FONT_SIZE_HUGE = 40
    private const val FONT_SIZE_GIANT = 48

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var prevTextView: TextView? = null
    private var currentTextView: TextView? = null
    private var nextTextView: TextView? = null
    private var next2TextView: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var initialY: Int = 0
    private var initialTouchY: Float = 0f
    private var isLongPress: Boolean = false
    private var lastColor: Int = 0

    @JvmStatic
    fun hide() {
        overlayView?.let {
            windowManager?.removeView(it)
        }
        windowManager = null
        overlayView = null
        prevTextView = null
        currentTextView = null
        nextTextView = null
        next2TextView = null
        layoutParams = null
        lastColor = 0
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    fun show(context: Context, prevLine: String?, currentLine: String?, nextLine1: String?, nextLine2: String?) {
        if (!Preferences.isDesktopLyricsEnabled()) {
            hide()
            return
        }

        if (overlayView == null) {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            overlayView = android.view.LayoutInflater.from(context).inflate(R.layout.desktop_lyrics_overlay, null)
            prevTextView = overlayView?.findViewById(R.id.desktop_lyrics_prev)
            currentTextView = overlayView?.findViewById(R.id.desktop_lyrics_current)
            nextTextView = overlayView?.findViewById(R.id.desktop_lyrics_next)
            next2TextView = overlayView?.findViewById(R.id.desktop_lyrics_next2)

            val screenHeight = context.resources.displayMetrics.heightPixels
            val savedY = Preferences.getDesktopLyricsPosition()
            val yPosition = if (savedY > 0) screenHeight - savedY else 0

            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = yPosition
            }

            setupTouchListener(context)
            windowManager?.addView(overlayView, layoutParams)
        }

        updateLyrics(prevLine, currentLine, nextLine1, nextLine2)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener(context: Context) {
        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                isLongPress = true
                val intent = Intent(context, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                context.startActivity(intent)
                hide()
            }
        })

        overlayView?.setOnTouchListener { view, event ->
            gestureDetector.onTouchEvent(event)

            if (isLongPress) {
                isLongPress = false
                when (event.action) {
                    MotionEvent.ACTION_UP -> view.performClick()
                }
                true
            } else {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialY = layoutParams?.y ?: 0
                        initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaY = (event.rawY - initialTouchY).toInt()
                        layoutParams?.y = initialY + deltaY
                        windowManager?.updateViewLayout(overlayView, layoutParams)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val screenHeight = context.resources.displayMetrics.heightPixels
                        val newY = screenHeight - (layoutParams?.y ?: 0)
                        Preferences.setDesktopLyricsPosition(newY)
                        view.performClick()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    fun updateLyrics(prevLine: String?, currentLine: String?, nextLine1: String?, nextLine2: String?) {
        if (!Preferences.isDesktopLyricsEnabled()) {
            hide()
            return
        }

        val lineCount = Preferences.getDesktopLyricsLineCount()

        overlayView?.let { view ->
            val bgAlpha = Preferences.getDesktopLyricsBgAlpha()
            val bgColor = Color.argb(bgAlpha * 255 / 100, 0, 0, 0)
            view.setBackgroundColor(bgColor)

            val color = Preferences.getDesktopLyricsColor()
            val fontSize = getFontSize(Preferences.getDesktopLyricsFontSize())
            val nextColor = Color.argb(Color.alpha(color) / 2, Color.red(color), Color.green(color), Color.blue(color))
            val farNextColor = Color.argb(Color.alpha(color) / 3, Color.red(color), Color.green(color), Color.blue(color))

            prevTextView?.apply {
                text = prevLine ?: ""
                setTextColor(nextColor)
                textSize = fontSize * 0.7f
                visibility = if (lineCount >= 3 && !prevLine.isNullOrEmpty()) View.VISIBLE else View.GONE
            }

            currentTextView?.apply {
                text = currentLine ?: ""
                setTextColor(color)
                textSize = fontSize.toFloat()
                visibility = if (currentLine.isNullOrEmpty()) View.GONE else View.VISIBLE
            }

            nextTextView?.apply {
                text = nextLine1 ?: ""
                setTextColor(nextColor)
                textSize = fontSize * 0.7f
                visibility = if (lineCount >= 2 && !nextLine1.isNullOrEmpty()) View.VISIBLE else View.GONE
            }

            next2TextView?.apply {
                text = nextLine2 ?: ""
                setTextColor(farNextColor)
                textSize = fontSize * 0.6f
                visibility = if (lineCount >= 4 && !nextLine2.isNullOrEmpty()) View.VISIBLE else View.GONE
            }

            lastColor = color
        }
    }

    private fun getFontSize(sizeIndex: Int): Int {
        return when (sizeIndex) {
            0 -> FONT_SIZE_TINY
            1 -> FONT_SIZE_SMALL
            2 -> FONT_SIZE_MEDIUM
            3 -> FONT_SIZE_LARGE
            4 -> FONT_SIZE_HUGE
            5 -> FONT_SIZE_GIANT
            else -> FONT_SIZE_MEDIUM
        }
    }
}
