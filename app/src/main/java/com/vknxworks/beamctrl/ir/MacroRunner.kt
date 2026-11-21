package com.vknxworks.beamctrl.ir

import com.vknxworks.beamctrl.data.Macro
import com.vknxworks.beamctrl.data.RemoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Runs a macro: each step's button is sent (waiting until it has gone out) and then the step's pause. One macro
 * at a time; starting another stops the one running. Steps whose remote or button is gone are skipped.
 */
class MacroRunner(private val scope: CoroutineScope, private val remotes: RemoteRepository, private val blaster: Blaster) {
    private val _running = MutableStateFlow<String?>(null)
    /** The id of the macro being run, or null. */
    val running: StateFlow<String?> = _running
    private var job: Job? = null

    fun run(macro: Macro) {
        job?.cancel()
        job = scope.launch {
            _running.value = macro.id
            try {
                macro.steps.forEachIndexed { i, step ->
                    val r = remotes.get(step.remoteId)
                    val button = r?.buttons?.firstOrNull { it.id == step.buttonId }
                    if (r != null && button != null) {
                        blaster.sendAndWait(button.code)
                        remotes.recordAc(r.id, button)
                    }
                    if (i < macro.steps.lastIndex) delay(step.delayMs.toLong())
                }
            } finally {
                if (_running.value == macro.id) _running.value = null
            }
        }
    }

    fun stop() {
        job?.cancel()
        _running.value = null
    }
}
