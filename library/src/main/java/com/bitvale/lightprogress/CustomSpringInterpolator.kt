package com.bitvale.lightprogress

import android.view.animation.Interpolator
import kotlin.math.pow
import kotlin.math.sin

/**
 * Created by https://github.com/geetgobindsingh
 * https://github.com/geetgobindsingh/AndroidAnimationInterpolator
 */
class CustomSpringInterpolator(private var factor: Float) : Interpolator {

    override fun getInterpolation(input: Float): Float {
        return (2.0.pow(-6.5 * input) * sin(2 * Math.PI * (input - factor / 4) / factor) + 1).toFloat()
    }
}