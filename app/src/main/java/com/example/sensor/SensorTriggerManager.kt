package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SensorTriggerState(
    val accelX: Float = 0f,
    val accelY: Float = 9.8f,
    val accelZ: Float = 0f,
    val proximity: Float = 5f,
    val isUpsideDown: Boolean = false,
    val isProximityCovered: Boolean = false,
    val isConditionMet: Boolean = false,
    val progress: Float = 0f, // 0.0 to 1.0
    val isUnlocked: Boolean = false
)

class SensorTriggerManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onTriggerSuccess: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private val _state = MutableStateFlow(SensorTriggerState())
    val state: StateFlow<SensorTriggerState> = _state.asStateFlow()

    private var conditionStartTime: Long = 0L
    private var progressJob: Job? = null
    private var isTriggerFired = false

    private val REQUIRED_HOLD_MS = 2000L
    private val ACCEL_Y_THRESHOLD = -8.0f // Upside down threshold

    fun startListening() {
        isTriggerFired = false
        sensorManager?.let { sm ->
            accelerometer?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            proximitySensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
        progressJob?.cancel()
        progressJob = null
        conditionStartTime = 0L
        _state.value = _state.value.copy(progress = 0f, isConditionMet = false)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (isTriggerFired) return

        val current = _state.value
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val upsideDown = y <= ACCEL_Y_THRESHOLD
                val conditionMet = upsideDown && current.isProximityCovered
                _state.value = current.copy(
                    accelX = x,
                    accelY = y,
                    accelZ = z,
                    isUpsideDown = upsideDown,
                    isConditionMet = conditionMet
                )
                checkCondition(conditionMet)
            }
            Sensor.TYPE_PROXIMITY -> {
                val prox = event.values[0]
                val maxRange = event.sensor.maximumRange
                // Proximity sensor: 0 cm or less than maxRange indicates covered
                val covered = prox == 0.0f || (maxRange > 1f && prox <= 1.0f)
                val conditionMet = current.isUpsideDown && covered
                _state.value = current.copy(
                    proximity = prox,
                    isProximityCovered = covered,
                    isConditionMet = conditionMet
                )
                checkCondition(conditionMet)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun checkCondition(isMet: Boolean) {
        if (isMet) {
            if (conditionStartTime == 0L) {
                conditionStartTime = System.currentTimeMillis()
                startProgressTracker()
            }
        } else {
            conditionStartTime = 0L
            progressJob?.cancel()
            progressJob = null
            _state.value = _state.value.copy(progress = 0f)
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (conditionStartTime > 0L) {
                val elapsed = System.currentTimeMillis() - conditionStartTime
                val currentProgress = (elapsed.toFloat() / REQUIRED_HOLD_MS).coerceIn(0f, 1f)
                _state.value = _state.value.copy(progress = currentProgress)

                if (elapsed >= REQUIRED_HOLD_MS) {
                    triggerSuccess()
                    break
                }
                delay(50)
            }
        }
    }

    /**
     * Executes the unlock event: haptic feedback, state update, and callback.
     */
    fun triggerSuccess() {
        if (isTriggerFired) return
        isTriggerFired = true
        vibrateGentle()
        _state.value = _state.value.copy(progress = 1f, isUnlocked = true)
        stopListening()
        onTriggerSuccess()
    }

    /**
     * Simulates the 2-second hold trigger for testing or environments lacking physical sensors.
     */
    fun simulateTrigger() {
        scope.launch(Dispatchers.Main) {
            _state.value = _state.value.copy(
                accelY = -9.2f,
                proximity = 0.0f,
                isUpsideDown = true,
                isProximityCovered = true,
                isConditionMet = true
            )
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < REQUIRED_HOLD_MS) {
                val elapsed = System.currentTimeMillis() - startTime
                _state.value = _state.value.copy(progress = elapsed.toFloat() / REQUIRED_HOLD_MS)
                delay(50)
            }
            triggerSuccess()
        }
    }

    private fun vibrateGentle() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 100, 80, 140),
                        intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(180, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(180)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resetUnlockState() {
        isTriggerFired = false
        _state.value = _state.value.copy(isUnlocked = false, progress = 0f)
        startListening()
    }
}
