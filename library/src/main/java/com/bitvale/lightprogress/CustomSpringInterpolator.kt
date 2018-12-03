package com.bitvale.lightprogress

import android.view.animation.Interpolator

/**
 * Created by https://github.com/geetgobindsingh
 * https://github.com/geetgobindsingh/AndroidAnimationInterpolator
 */
class CustomSpringInterpolator(private var factor: Float) : Interpolator {

    override fun getInterpolation(input: Float): Float {
        return (Math.pow(2.0, -6.5 * input) * Math.sin(2 * Math.PI * (input - factor / 4) / factor) + 1).toFloat()
    }
}