package com.deepkush.reprange.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.annotation.SuppressLint
import android.os.VibratorManager
import android.view.HapticFeedbackConstants as HFC
import android.view.View

@SuppressLint("NewApi")
object AppHaptics {

    @Volatile
    private var enabled = true

    private val primitives = intArrayOf(
        VibrationEffect.Composition.PRIMITIVE_TICK,
        VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
        VibrationEffect.Composition.PRIMITIVE_CLICK,
        VibrationEffect.Composition.PRIMITIVE_THUD,
        VibrationEffect.Composition.PRIMITIVE_SLOW_RISE,
    )

    private lateinit var support: BooleanArray
    private var vibrator: Vibrator? = null

    fun init(context: Context) {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { support = vibrator!!.arePrimitivesSupported(*primitives) }
        }
    }

    fun setEnabled(value: Boolean) {
        enabled = value
    }

    private val active: Vibrator? get() = vibrator?.takeIf { enabled }

    private fun supports(vararg idx: Int): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            ::support.isInitialized &&
            idx.all { support.getOrNull(it) == true }

    private fun idxOf(p: Int) = primitives.indexOf(p)

    private fun compose(vararg parts: Triple<Int, Float, Int>): Boolean {
        val vib = active ?: return false
        if (!supports(*parts.map { idxOf(it.first) }.toIntArray())) return false
        return runCatching {
            val c = VibrationEffect.startComposition()
            parts.forEach { (p, scale, delay) -> c.addPrimitive(p, scale, delay) }
            vib.vibrate(c.compose())
            true
        }.getOrDefault(false)
    }

    private fun predefined(effect: Int): Boolean {
        val vib = active ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            vib.vibrate(VibrationEffect.createPredefined(effect))
            true
        }.getOrDefault(false)
    }

    private fun viewFallback(view: View?, constant: Int) {
        view?.performHapticFeedback(constant)
    }

    /** #1 Tap/select button or row - subtle. */
    fun tap(view: View?) {
        if (compose(Triple(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 0))) return
        viewFallback(view, HFC.VIRTUAL_KEY)
    }

    /** #2 Long-press menu opens - decisive. */
    fun longPress(view: View?) {
        if (compose(Triple(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 0))) return
        viewFallback(view, HFC.LONG_PRESS)
    }

    /** #3 Secondary/context tap - light. */
    fun contextClick(view: View?) {
        if (compose(Triple(VibrationEffect.Composition.PRIMITIVE_TICK, 0.7f, 0))) return
        viewFallback(view, HFC.CONTEXT_CLICK)
    }

    /** #4 Toggle ON - medium. */
    fun toggleOn(view: View?) {
        if (compose(Triple(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.7f, 0))) return
        if (Build.VERSION.SDK_INT >= 30) viewFallback(view, HFC.CONFIRM)
        else viewFallback(view, HFC.VIRTUAL_KEY)
    }

    /** #5 Toggle OFF - light-med. */
    fun toggleOff(view: View?) {
        if (compose(Triple(VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f, 0))) return
        viewFallback(view, HFC.VIRTUAL_KEY)
    }

    /** #6/#23 Discrete step / tab switch / chip switch - very light. */
    fun segmentTick(view: View?) {
        if (compose(Triple(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f, 0))) return
        viewFallback(view, HFC.CLOCK_TICK)
    }

    /** #7 Continuous fine scrub. */
    fun scrubTick(view: View?) {
        if (compose(Triple(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.5f, 0))) return
        viewFallback(view, HFC.CLOCK_TICK)
    }

    /** #8 Slider boundary hit / gesture end landing. */
    fun gestureEnd(view: View?) {
        if (compose(Triple(VibrationEffect.Composition.PRIMITIVE_THUD, 0.7f, 0))) return
        if (Build.VERSION.SDK_INT >= 30) viewFallback(view, HFC.GESTURE_END)
        else viewFallback(view, HFC.CLOCK_TICK)
    }

    /** #9 Drag-reorder pickup / threshold armed (#13, #21). */
    fun gestureArm(view: View?) {
        if (compose(Triple(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 1.0f, 0))) return
        if (Build.VERSION.SDK_INT >= 30) viewFallback(view, HFC.GESTURE_START)
        else viewFallback(view, HFC.LONG_PRESS)
    }

    /** #10 Dragged item crosses slot - ripple. */
    fun dragTick(view: View?) {
        if (compose(Triple(VibrationEffect.Composition.PRIMITIVE_TICK, 0.4f, 0))) return
        viewFallback(view, HFC.CLOCK_TICK)
    }

    /** #11 Drop/reorder commit - strong close. */
    fun dropCommit(view: View?) {
        if (compose(
                Triple(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 0),
                Triple(VibrationEffect.Composition.PRIMITIVE_THUD, 0.5f, 50),
            )
        ) return
        confirm(view)
    }

    /** #12/#19 Swipe/paging commit - impact. */
    fun swipeCommit(view: View?) {
        if (compose(Triple(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f, 0))) return
        if (Build.VERSION.SDK_INT >= 30) viewFallback(view, HFC.CONFIRM)
        else viewFallback(view, HFC.VIRTUAL_KEY)
    }

    /** #15 Success completes - rising confirm. */
    fun confirm(view: View?) {
        if (compose(
                Triple(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 0.8f, 0),
                Triple(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 50),
            )
        ) return
        if (Build.VERSION.SDK_INT >= 30) viewFallback(view, HFC.CONFIRM)
        else viewFallback(view, HFC.VIRTUAL_KEY)
    }

    /** #16 Error/failure - harsh double. */
    fun reject(view: View?) {
        if (compose(
                Triple(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f, 0),
                Triple(VibrationEffect.Composition.PRIMITIVE_THUD, 0.6f, 80),
            )
        ) return
        if (Build.VERSION.SDK_INT >= 30) viewFallback(view, HFC.REJECT)
        else if (Build.VERSION.SDK_INT >= 29 && !predefined(HapticEffects.DOUBLE_CLICK)) {
            viewFallback(view, HFC.LONG_PRESS)
        }
    }

    /** #17 Destructive confirm - heavy. */
    fun thud(view: View?) {
        if (compose(Triple(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f, 0))) return
        if (Build.VERSION.SDK_INT >= 29 && predefined(HapticEffects.HEAVY_CLICK)) return
        viewFallback(view, HFC.LONG_PRESS)
    }

    /** #18 Primary action press - crisp. */
    fun primaryTap(view: View?) {
        if (compose(Triple(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.9f, 0))) return
        viewFallback(view, HFC.VIRTUAL_KEY)
    }

    private object HapticEffects {
        const val HEAVY_CLICK = 5 // VibrationEffect.EFFECT_HEAVY_CLICK
        const val DOUBLE_CLICK = 1 // VibrationEffect.EFFECT_DOUBLE_CLICK
    }
}
