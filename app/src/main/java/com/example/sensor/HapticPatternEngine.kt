package com.example.sensor

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * Physically-modeled tactile vibration engine for FlipYear.
 *
 * Uses Android's hardware `VibratorManager` / `Vibrator` APIs with:
 * - Rich `VibrationEffect.Composition` primitives on API 30+/31+ when supported by the motor
 * - Custom multi-stage amplitude waveforms on API 26+ (respecting `hasAmplitudeControl()`)
 * - Legacy timed vibration fallback on API 24-25
 * - Graceful `View.performHapticFeedback` fallback when running on devices/emulators without a vibrator
 */
class HapticPatternEngine(
    context: Context,
    private val view: View?
) {
    private val vibrator: Vibrator? = resolveVibrator(context.applicationContext)

    /**
     * Subtle crisp paper-leaf detent when a 3D calendar page crosses its hinge midpoint
     * or when stepping along the curved Arc timeline dial.
     */
    fun pageFlipLift() {
        val vib = vibrator
        if (vib == null || !vib.hasVibrator()) {
            performViewFallback(HapticFeedbackConstants.CLOCK_TICK)
            return
        }

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vib.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)
            ) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.48f)
                    .compose()
                vib.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amp = if (vib.hasAmplitudeControl()) 78 else VibrationEffect.DEFAULT_AMPLITUDE
                vib.vibrate(VibrationEffect.createOneShot(12L, amp))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(12L)
            }
        }.onFailure {
            performViewFallback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    /**
     * Tactile two-beat paper rustle + gentle page settle when flipping to a new month
     * in the 3D calendar flipbook.
     */
    fun pageFlipSettle() {
        val vib = vibrator
        if (vib == null || !vib.hasVibrator()) {
            performViewFallback(HapticFeedbackConstants.CONTEXT_CLICK)
            return
        }

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vib.areAllPrimitivesSupported(
                    VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                    VibrationEffect.Composition.PRIMITIVE_CLICK
                )
            ) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.36f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.64f, 26)
                    .compose()
                vib.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0L, 10L, 26L, 16L)
                val effect = if (vib.hasAmplitudeControl()) {
                    VibrationEffect.createWaveform(timings, intArrayOf(0, 68, 0, 145), -1)
                } else {
                    VibrationEffect.createWaveform(timings, -1)
                }
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(longArrayOf(0L, 10L, 26L, 14L), -1)
            }
        }.onFailure {
            performViewFallback(HapticFeedbackConstants.CONTEXT_CLICK)
        }
    }

    /**
     * Mechanical threshold detent fired the moment a vertical drag crosses the commit line
     * for swipe-up delete (`isTrash = true`) or swipe-down keep (`isTrash = false`).
     */
    fun swipeThresholdDetent(isTrash: Boolean) {
        val vib = vibrator
        if (vib == null || !vib.hasVibrator()) {
            performViewFallback(
                if (isTrash) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.CLOCK_TICK
            )
            return
        }

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val primitive = if (isTrash) {
                    VibrationEffect.Composition.PRIMITIVE_CLICK
                } else {
                    VibrationEffect.Composition.PRIMITIVE_TICK
                }
                if (vib.areAllPrimitivesSupported(primitive)) {
                    val scale = if (isTrash) 0.80f else 0.56f
                    val effect = VibrationEffect.startComposition()
                        .addPrimitive(primitive, scale)
                        .compose()
                    vib.vibrate(effect)
                    return
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val duration = if (isTrash) 18L else 13L
                val amp = if (vib.hasAmplitudeControl()) {
                    if (isTrash) 180 else 115
                } else {
                    VibrationEffect.DEFAULT_AMPLITUDE
                }
                vib.vibrate(VibrationEffect.createOneShot(duration, amp))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(if (isTrash) 18L else 12L)
            }
        }.onFailure {
            performViewFallback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    /**
     * Authoritative two-stage descending "whoosh-thud" vibration pattern confirming
     * swipe-to-delete or batch trash actions.
     */
    fun swipeDeleteConfirm() {
        val vib = vibrator
        if (vib == null || !vib.hasVibrator()) {
            performViewFallback(HapticFeedbackConstants.LONG_PRESS)
            return
        }

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                vib.areAllPrimitivesSupported(
                    VibrationEffect.Composition.PRIMITIVE_CLICK,
                    VibrationEffect.Composition.PRIMITIVE_THUD
                )
            ) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.85f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.96f, 34)
                    .compose()
                vib.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0L, 20L, 36L, 42L)
                val effect = if (vib.hasAmplitudeControl()) {
                    VibrationEffect.createWaveform(timings, intArrayOf(0, 175, 0, 235), -1)
                } else {
                    VibrationEffect.createWaveform(timings, -1)
                }
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(longArrayOf(0L, 20L, 36L, 40L), -1)
            }
        }.onFailure {
            performViewFallback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /**
     * Crisp uplifting double-tap confirmation when keeping or favoriting a memory.
     */
    fun swipeKeepConfirm() {
        val vib = vibrator
        if (vib == null || !vib.hasVibrator()) {
            performViewFallback(HapticFeedbackConstants.CONTEXT_CLICK)
            return
        }

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vib.areAllPrimitivesSupported(
                    VibrationEffect.Composition.PRIMITIVE_TICK,
                    VibrationEffect.Composition.PRIMITIVE_CLICK
                )
            ) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.50f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.76f, 28)
                    .compose()
                vib.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0L, 12L, 28L, 18L)
                val effect = if (vib.hasAmplitudeControl()) {
                    VibrationEffect.createWaveform(timings, intArrayOf(0, 96, 0, 168), -1)
                } else {
                    VibrationEffect.createWaveform(timings, -1)
                }
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(longArrayOf(0L, 12L, 28L, 16L), -1)
            }
        }.onFailure {
            performViewFallback(HapticFeedbackConstants.CONTEXT_CLICK)
        }
    }

    /**
     * Quick ascending triple-ripple confirming shake-to-undo or vault restore.
     */
    fun undoPulse() {
        val vib = vibrator
        if (vib == null || !vib.hasVibrator()) {
            performViewFallback(HapticFeedbackConstants.LONG_PRESS)
            return
        }

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0L, 12L, 24L, 14L, 24L, 22L)
                val effect = if (vib.hasAmplitudeControl()) {
                    VibrationEffect.createWaveform(timings, intArrayOf(0, 88, 0, 142, 0, 205), -1)
                } else {
                    VibrationEffect.createWaveform(timings, -1)
                }
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(longArrayOf(0L, 12L, 24L, 14L, 24L, 20L), -1)
            }
        }.onFailure {
            performViewFallback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /**
     * Soft hardcover spine click when swiping between 3D Yearbooks on the Bookshelf.
     */
    fun bookshelfDetent() {
        val vib = vibrator
        if (vib == null || !vib.hasVibrator()) {
            performViewFallback(HapticFeedbackConstants.CLOCK_TICK)
            return
        }

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vib.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)
            ) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.62f)
                    .compose()
                vib.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amp = if (vib.hasAmplitudeControl()) 122 else VibrationEffect.DEFAULT_AMPLITUDE
                vib.vibrate(VibrationEffect.createOneShot(16L, amp))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(16L)
            }
        }.onFailure {
            performViewFallback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    private fun performViewFallback(feedbackConstant: Int) {
        runCatching {
            view?.performHapticFeedback(feedbackConstant)
        }
    }

    companion object {
        private fun resolveVibrator(context: Context): Vibrator? {
            return runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    manager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
            }.getOrNull()
        }
    }
}

@Composable
fun rememberHapticEngine(): HapticPatternEngine {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view) {
        HapticPatternEngine(context, view)
    }
}
