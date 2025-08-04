package com.example.fittrack.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class StepCounterManager(
    private val context: Context,
    private val onStepCountChanged: (Int) -> Unit
) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private var initialStepCount = -1
    private var currentStepCount = 0
    private var isTracking = false

    // ✅ NUEVAS VARIABLES PARA PAUSE/RESUME
    private var isPaused = false
    private var pausedStepCount = 0

    private val TAG = "StepCounterManager"

    fun startTracking() {
        if (stepCounterSensor != null) {
            Log.d(TAG, "Iniciando contador de pasos con TYPE_STEP_COUNTER")
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)
            isTracking = true
            isPaused = false
        } else if (stepDetectorSensor != null) {
            Log.d(TAG, "TYPE_STEP_COUNTER no disponible, usando TYPE_STEP_DETECTOR")
            sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI)
            isTracking = true
            isPaused = false
        } else {
            Log.w(TAG, "No hay sensores de pasos disponibles en este dispositivo")
        }
    }

    fun stopTracking() {
        if (isTracking) {
            sensorManager.unregisterListener(this)
            isTracking = false
            isPaused = false
            Log.d(TAG, "Contador de pasos detenido")
        }
    }

    // ✅ NUEVO MÉTODO: pauseTracking()
    fun pauseTracking() {
        if (isTracking && !isPaused) {
            isPaused = true
            pausedStepCount = currentStepCount
            Log.d(TAG, "Contador de pasos pausado en: $pausedStepCount pasos")
        } else {
            Log.w(TAG, "No se puede pausar: isTracking=$isTracking, isPaused=$isPaused")
        }
    }

    // ✅ NUEVO MÉTODO: resumeTracking()
    fun resumeTracking() {
        if (isTracking && isPaused) {
            isPaused = false
            Log.d(TAG, "Contador de pasos reanudado desde: $pausedStepCount pasos")
        } else {
            Log.w(TAG, "No se puede reanudar: isTracking=$isTracking, isPaused=$isPaused")
        }
    }

    fun resetStepCount() {
        initialStepCount = -1
        currentStepCount = 0
        pausedStepCount = 0
        isPaused = false
        Log.d(TAG, "Contador de pasos reiniciado")
    }

    fun getCurrentStepCount(): Int {
        return currentStepCount
    }

    // ✅ NUEVOS MÉTODOS DE ESTADO
    fun isCurrentlyTracking(): Boolean {
        return isTracking && !isPaused
    }

    fun isPaused(): Boolean {
        return isPaused
    }

    fun getTrackingStatus(): String {
        return when {
            !isTracking -> "Detenido"
            isPaused -> "Pausado"
            else -> "Activo"
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        // ✅ MODIFICADO: No procesar eventos cuando está pausado
        if (isPaused) {
            Log.d(TAG, "Sensor pausado - ignorando evento")
            return
        }

        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_STEP_COUNTER -> {
                    // TYPE_STEP_COUNTER da el total acumulado desde el reinicio del dispositivo
                    val totalSteps = it.values[0].toInt()
                    if (initialStepCount == -1) {
                        // Primera lectura, establecer como punto de inicio
                        initialStepCount = totalSteps
                        currentStepCount = 0
                    } else {
                        // Calcular pasos desde que iniciamos el tracking
                        currentStepCount = totalSteps - initialStepCount
                    }
                    Log.d(TAG, "Pasos detectados: $currentStepCount (Total del sistema: $totalSteps)")
                    onStepCountChanged(currentStepCount)
                }

                Sensor.TYPE_STEP_DETECTOR -> {
                    // TYPE_STEP_DETECTOR detecta cada paso individual
                    currentStepCount++
                    Log.d(TAG, "Paso detectado. Total: $currentStepCount")
                    onStepCountChanged(currentStepCount)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Precisión del sensor cambiada: $accuracy")
    }

    fun isStepCountingSupported(): Boolean {
        return stepCounterSensor != null || stepDetectorSensor != null
    }

    fun getSensorInfo(): String {
        val basicInfo = when {
            stepCounterSensor != null -> "Usando TYPE_STEP_COUNTER: ${stepCounterSensor.name}"
            stepDetectorSensor != null -> "Usando TYPE_STEP_DETECTOR: ${stepDetectorSensor.name}"
            else -> "Sin sensores de pasos disponibles"
        }

        return buildString {
            append(basicInfo)
            append("\nEstado: ${getTrackingStatus()}")
            append("\nPasos actuales: $currentStepCount")
            if (isPaused) {
                append("\nPasos al pausar: $pausedStepCount")
            }
        }
    }

    // ✅ NUEVO MÉTODO: Obtener estadísticas detalladas
    fun getDetailedStats(): String {
        return buildString {
            append("=== STEP COUNTER MANAGER STATS ===\n")
            append("Soporte de sensores: ${isStepCountingSupported()}\n")
            append("Estado actual: ${getTrackingStatus()}\n")
            append("Tracking activo: $isTracking\n")
            append("Pausado: $isPaused\n")
            append("Pasos actuales: $currentStepCount\n")
            append("Paso inicial (sistema): $initialStepCount\n")
            if (isPaused) {
                append("Pasos al pausar: $pausedStepCount\n")
            }
            append("Sensor info: ${getSensorInfo()}")
        }
    }
}