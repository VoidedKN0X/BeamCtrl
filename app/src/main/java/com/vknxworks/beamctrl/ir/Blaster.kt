package com.vknxworks.beamctrl.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** A button currently held down. Call [release] when the finger lifts. */
fun interface Press {
    fun release()
}

/**
 * The phone's IR blaster. Every transmission goes through one background thread, so presses
 * are sent in order and never overlap.
 */
class Blaster(context: Context) {
    private val ir: ConsumerIrManager? = context.getSystemService(ConsumerIrManager::class.java)
    private val dispatcher = Executors.newSingleThreadExecutor { r -> Thread(r, "beamctrl-ir") }.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var toggle = false

    /** Whether this phone has an IR emitter that apps can use. */
    val available: Boolean = runCatching { ir?.hasIrEmitter() == true }.getOrDefault(false)

    /** Carrier frequencies the emitter supports, in Hz. Empty when the phone doesn't say. */
    val carrierRanges: List<IntRange> = if (!available) emptyList() else runCatching {
        ir?.carrierFrequencies?.map { it.minFrequency..it.maxFrequency }.orEmpty()
    }.getOrDefault(emptyList())

    private val _beams = MutableSharedFlow<Unit>(extraBufferCapacity = 16)

    /** Fires every time a signal leaves the emitter (drives the beam light in the UI). */
    val beams: SharedFlow<Unit> = _beams

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors: SharedFlow<String> = _errors

    /** Sends [code] once. */
    fun send(code: IrCode) {
        press(code, repeat = false).release()
    }

    /**
     * Starts sending [code]. With [repeat], holding the button for more than a moment keeps it
     * going (the repeat frames NEC remotes send, or the full code again for other protocols)
     * until [Press.release], for up to [MAX_HOLD_MS].
     */
    fun press(code: IrCode, repeat: Boolean): Press {
        val released = AtomicBoolean(false)
        val pressedAt = SystemClock.uptimeMillis()
        scope.launch {
            if (!available) {
                _errors.tryEmit("This phone doesn't have an IR blaster")
                return@launch
            }
            val signal = try {
                if (code.usesToggle) toggle = !toggle
                code.encode(toggle)
            } catch (e: Exception) {
                _errors.tryEmit("Can't send this code: ${e.message ?: "invalid"}")
                return@launch
            }
            if (!transmit(signal.frequency, signal.once)) return@launch
            if (!repeat) return@launch
            // A quick tap is one press; only start repeating once the button has been held.
            while (!released.get() && SystemClock.uptimeMillis() - pressedAt < HOLD_DELAY_MS) delay(15)
            while (!released.get() && isActive && SystemClock.uptimeMillis() - pressedAt < MAX_HOLD_MS) {
                if (!transmit(signal.frequency, signal.repeat)) break
            }
        }
        return Press { released.set(true) }
    }

    private suspend fun transmit(frequency: Int, pattern: IntArray): Boolean {
        val manager = ir ?: return false
        val start = SystemClock.uptimeMillis()
        try {
            manager.transmit(fitFrequency(frequency), pattern)
        } catch (e: Exception) {
            _errors.tryEmit("The IR blaster refused that signal")
            return false
        }
        _beams.tryEmit(Unit)
        // Some phones return from transmit() straight away, others block until it's sent.
        // Either way, wait out the signal (plus a gap if it ends with the LED on) before the next one.
        val durationMs = pattern.sum() / 1000L + if (pattern.size % 2 == 1) 40 else 0
        val remaining = durationMs - (SystemClock.uptimeMillis() - start)
        if (remaining > 0) delay(remaining)
        return true
    }

    /** Uses the requested carrier if the emitter supports it, otherwise the closest one it does. */
    private fun fitFrequency(frequency: Int): Int {
        if (carrierRanges.isEmpty() || carrierRanges.any { frequency in it }) return frequency
        return carrierRanges
            .flatMap { listOf(it.first, it.last) }
            .minByOrNull { kotlin.math.abs(it - frequency) } ?: frequency
    }

    companion object {
        private const val HOLD_DELAY_MS = 450L
        private const val MAX_HOLD_MS = 15_000L
    }
}
