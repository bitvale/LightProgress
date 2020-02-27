package com.bitvale.lightprogress

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getDimensionOrThrow
import androidx.core.content.res.getStringOrThrow
import androidx.core.graphics.withRotation

/**
 * Created by Alexander Kolpakov on 11/30/2018
 */
class LightProgress @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val ANIMATION_DURATION = 1800L
        private const val INTERPOLATOR_FACTOR = 0.6f
        private const val FULL_CIRCLE = 360f
        private const val LIGHT_LETTER = "i"
    }

    private lateinit var text: String
    private lateinit var textLayout: StaticLayout

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val lightPath = Path()

    private lateinit var textBitmap: Bitmap

    private var lightPivotX = 0f
    private var lightPivotY = 0f
    private var letterWidth = 0

    private var angle = 0f
        set(value) {
            field = value
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                postInvalidateOnAnimation()
            } else {
                invalidate()
            }
        }

    private var animator = ValueAnimator.ofFloat(0f, 1f).apply {
        addUpdateListener {
            val value = it.animatedValue as Float
            angle = lerp(0f, FULL_CIRCLE, value)
        }
        interpolator = CustomSpringInterpolator(INTERPOLATOR_FACTOR)
        repeatMode = ValueAnimator.RESTART
        repeatCount = ValueAnimator.INFINITE
        duration = ANIMATION_DURATION
    }

    init {
        attrs?.let { retrieveAttributes(attrs, defStyleAttr) }
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    private fun retrieveAttributes(attrs: AttributeSet, defStyleAttr: Int) {
        val typedArray =
            context.obtainStyledAttributes(
                attrs, R.styleable.LightProgress, defStyleAttr,
                R.style.LightProgress
            )

        text = typedArray.getStringOrThrow(R.styleable.LightProgress_android_text)

        textPaint.apply {
            color = typedArray.getColorOrThrow(R.styleable.LightProgress_android_textColor)
            textSize = typedArray.getDimensionOrThrow(R.styleable.LightProgress_android_textSize)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_ATOP)
        }

        textLayout = createLayout(text)

        lightPaint.color = typedArray.getColorOrThrow(R.styleable.LightProgress_light_color)

        typedArray.recycle()
    }

    private fun textToBitmap(text: String): Bitmap {
        val baseline = -textPaint.ascent()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawText(text, 0f, baseline, textPaint)
        return bitmap
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = textLayout.width
        val h = textLayout.height
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initLight()
        textBitmap = textToBitmap(text)
    }

    private fun initLight() {
        val textBounds = Rect()
        val iPos = text.indexOf(LIGHT_LETTER)
        if (iPos == -1) {
            lightPivotX = width / 2f
            lightPivotY = 0f
            textPaint.getTextBounds(text, 0, text.length - 1, textBounds)
        } else {
            val textWithLetter = text.substring(0, iPos + 1)
            val textBeforeLetter = text.substring(0, iPos)

            var textLayout = createLayout(textWithLetter)
            val withWithLetter = textLayout.width

            textLayout = createLayout(textBeforeLetter)
            val widthWithoutLetter = textLayout.width

            textPaint.getTextBounds(LIGHT_LETTER, 0, 1, textBounds)

            letterWidth = textBounds.width()// one "i" letter width

            textPaint.getTextBounds(text, 0, text.length - 1, textBounds)

            val letterWidthWithIndent = withWithLetter - widthWithoutLetter

            lightPivotX = withWithLetter - letterWidthWithIndent / 2f
            lightPivotY = ((textPaint.ascent() * -1) - textBounds.height()) + letterWidth / 2f
        }

        val topY = textPaint.ascent() * -1 - textBounds.height()
        lightPath.moveTo(lightPivotX - letterWidth / 2f, topY)
        lightPath.moveTo(lightPivotX + letterWidth / 2f, topY)
        lightPath.lineTo(lightPivotX + width / 2f, width.toFloat())
        lightPath.lineTo(lightPivotX - width / 2f, width.toFloat())
        lightPath.lineTo(lightPivotX - letterWidth / 2f, topY)
        lightPath.close()
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.withRotation(angle, lightPivotX, lightPivotY) {
            drawPath(lightPath, lightPaint)
        }
        canvas?.drawBitmap(textBitmap, 0f, 0f, textPaint)
    }

    private fun createLayout(text: String): StaticLayout {
        return text.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(
                    it,
                    0,
                    it.length,
                    textPaint,
                    textPaint.measureText(it).toInt()
                )
                    .build()
            } else {
                StaticLayout(
                    text,
                    textPaint,
                    textPaint.measureText(it).toInt(),
                    Layout.Alignment.ALIGN_CENTER,
                    1f,
                    0f,
                    true
                )
            }
        }
    }

    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t
    }

    /**
     * Start the light animation.
     */
    fun on() {
        animator?.start()
    }

    /**
     * Stop the light animation.
     */
    fun off() {
        animator?.cancel()
        angle = 0f
    }

    /**
     * @return Whether the light animation is currently running.
     */
    fun isOn() = animator?.isRunning == true
}