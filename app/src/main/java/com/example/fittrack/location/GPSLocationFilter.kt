package com.example.fittrack.location

import android.location.Location
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlin.math.*

class GPSLocationFilter {

    private val TAG = "GPSLocationFilter"

    // ✅ CONFIGURACIÓN MEJORADA PARA EVITAR LÍNEAS RARAS
    private val MIN_ACCURACY = 15f // metros - más estricto para mejor precisión
    private val MIN_DISTANCE_THRESHOLD = 8f // metros - distancia mínima más grande
    private val MAX_SPEED_THRESHOLD = 30f // km/h - más conservador para caminata/correr
    private val MIN_TIME_INTERVAL = 3000L // ms - intervalo más largo (3 segundos)
    private val STATIONARY_RADIUS = 12f // metros - radio más grande para detectar cuando estás parado
    private val SIGNIFICANT_MOVEMENT_DISTANCE = 15f // metros - distancia que realmente indica movimiento

    // ✅ NUEVOS PARÁMETROS PARA FILTRO AVANZADO
    private val MAX_ACCURACY_FOR_FIRST_LOCATION = 10f // Primera ubicación debe ser muy precisa
    private val SMOOTHING_WINDOW_SIZE = 3 // Ventana para promedio móvil
    private val MIN_CONSECUTIVE_VALID_READINGS = 3 // Lecturas consecutivas antes de aceptar movimiento

    private var lastValidLocation: Location? = null
    private var lastUpdateTime = 0L
    private var consecutiveStationaryCount = 0
    private val maxStationaryCount = 8 // Más lecturas antes de considerar movimiento

    // ✅ NUEVO: Buffer para promedio móvil
    private val locationBuffer = mutableListOf<Location>()
    private var consecutiveValidReadings = 0
    private var totalRejectedReadings = 0

    fun filterLocation(newLocation: Location): FilterResult {
        val currentTime = System.currentTimeMillis()
        totalRejectedReadings++ // Contador total

        Log.d(TAG, "=== ANALIZANDO NUEVA UBICACIÓN ===")
        Log.d(TAG, "Lat: ${newLocation.latitude}, Lng: ${newLocation.longitude}")
        Log.d(TAG, "Precisión: ${newLocation.accuracy}m")

        // 1. ✅ FILTRO DE PRECISIÓN MÁS ESTRICTO
        if (!newLocation.hasAccuracy()) {
            Log.d(TAG, "❌ Rechazado: Sin información de precisión")
            return FilterResult.REJECTED_LOW_ACCURACY
        }

        val lastLocation = lastValidLocation
        val requiredAccuracy = if (lastLocation == null) MAX_ACCURACY_FOR_FIRST_LOCATION else MIN_ACCURACY

        if (newLocation.accuracy > requiredAccuracy) {
            Log.d(TAG, "❌ Rechazado: Baja precisión (${newLocation.accuracy}m > ${requiredAccuracy}m)")
            return FilterResult.REJECTED_LOW_ACCURACY
        }

        // 2. ✅ FILTRO DE TIEMPO MÁS ESTRICTO
        if (currentTime - lastUpdateTime < MIN_TIME_INTERVAL) {
            Log.d(TAG, "❌ Rechazado: Intervalo muy corto (${currentTime - lastUpdateTime}ms < ${MIN_TIME_INTERVAL}ms)")
            return FilterResult.REJECTED_TIME_INTERVAL
        }

        // 3. ✅ PRIMERA UBICACIÓN - VALIDACIÓN ESPECIAL
        if (lastLocation == null) {
            Log.d(TAG, "🎯 Primera ubicación - validación especial")

            // Para la primera ubicación, ser muy exigente
            if (newLocation.accuracy <= MAX_ACCURACY_FOR_FIRST_LOCATION) {
                lastValidLocation = newLocation
                lastUpdateTime = currentTime
                consecutiveStationaryCount = 0
                consecutiveValidReadings = 1
                locationBuffer.clear()
                locationBuffer.add(newLocation)
                totalRejectedReadings = 0

                Log.d(TAG, "✅ Primera ubicación ACEPTADA (precisión: ${newLocation.accuracy}m)")
                return FilterResult.ACCEPTED_FIRST_LOCATION
            } else {
                Log.d(TAG, "❌ Primera ubicación rechazada - precisión insuficiente")
                return FilterResult.REJECTED_LOW_ACCURACY
            }
        }

        // 4. ✅ ANÁLISIS DE DISTANCIA Y MOVIMIENTO
        val distance = calculateDistance(lastLocation, newLocation)
        val timeElapsed = (currentTime - lastUpdateTime) / 1000f
        val speed = if (timeElapsed > 0) (distance / timeElapsed) * 3.6f else 0f

        Log.d(TAG, "Distancia: ${String.format("%.2f", distance)}m")
        Log.d(TAG, "Velocidad: ${String.format("%.2f", speed)} km/h")
        Log.d(TAG, "Tiempo transcurrido: ${String.format("%.1f", timeElapsed)}s")

        // 5. ✅ FILTRO DE VELOCIDAD IMPOSIBLE
        if (speed > MAX_SPEED_THRESHOLD) {
            Log.d(TAG, "❌ Rechazado: Velocidad imposible (${String.format("%.2f", speed)} km/h)")
            consecutiveValidReadings = 0
            return FilterResult.REJECTED_HIGH_SPEED
        }

        // 6. ✅ DETECCIÓN MEJORADA DE USUARIO ESTACIONARIO
        if (distance <= STATIONARY_RADIUS) {
            consecutiveStationaryCount++
            Log.d(TAG, "🚶 Posible usuario estacionario (${consecutiveStationaryCount}/${maxStationaryCount})")

            if (consecutiveStationaryCount >= maxStationaryCount) {
                Log.d(TAG, "❌ Rechazado: Usuario confirmado como estacionario")
                consecutiveValidReadings = 0
                return FilterResult.REJECTED_STATIONARY
            }
        } else {
            // Solo resetear si el movimiento es significativo
            if (distance >= SIGNIFICANT_MOVEMENT_DISTANCE) {
                consecutiveStationaryCount = 0
                Log.d(TAG, "🏃 Movimiento significativo detectado - reseteando contador estacionario")
            }
        }

        // 7. ✅ FILTRO DE DISTANCIA MÍNIMA MEJORADO
        if (distance < MIN_DISTANCE_THRESHOLD) {
            Log.d(TAG, "❌ Rechazado: Distancia muy pequeña (${String.format("%.2f", distance)}m)")
            consecutiveValidReadings = 0
            return FilterResult.REJECTED_SMALL_DISTANCE
        }

        // 8. ✅ REQUERIR LECTURAS CONSECUTIVAS VÁLIDAS
        consecutiveValidReadings++
        if (consecutiveValidReadings < MIN_CONSECUTIVE_VALID_READINGS) {
            Log.d(TAG, "⏳ Esperando más lecturas válidas (${consecutiveValidReadings}/${MIN_CONSECUTIVE_VALID_READINGS})")
            return FilterResult.REJECTED_SMALL_DISTANCE
        }

        // 9. ✅ APLICAR PROMEDIO MÓVIL PARA SUAVIZAR
        locationBuffer.add(newLocation)
        if (locationBuffer.size > SMOOTHING_WINDOW_SIZE) {
            locationBuffer.removeAt(0)
        }

        val smoothedLocation = if (locationBuffer.size >= 2) {
            calculateMovingAverage(locationBuffer)
        } else {
            newLocation
        }

        // 10. ✅ VALIDACIÓN FINAL CON LA UBICACIÓN SUAVIZADA
        val finalDistance = calculateDistance(lastLocation, smoothedLocation)
        if (finalDistance < MIN_DISTANCE_THRESHOLD) {
            Log.d(TAG, "❌ Rechazado: Distancia suavizada muy pequeña (${String.format("%.2f", finalDistance)}m)")
            return FilterResult.REJECTED_SMALL_DISTANCE
        }

        // ✅ UBICACIÓN ACEPTADA
        lastValidLocation = smoothedLocation
        lastUpdateTime = currentTime
        totalRejectedReadings = 0 // Reset contador de rechazos

        Log.d(TAG, "✅ UBICACIÓN ACEPTADA Y SUAVIZADA")
        Log.d(TAG, "Distancia final: ${String.format("%.2f", finalDistance)}m")
        Log.d(TAG, "Lecturas válidas consecutivas: $consecutiveValidReadings")

        return FilterResult.ACCEPTED_AND_SMOOTHED(smoothedLocation)
    }

    // ✅ NUEVO: Cálculo de promedio móvil para suavizar ubicaciones
    private fun calculateMovingAverage(locations: List<Location>): Location {
        if (locations.isEmpty()) return locations.last()

        var totalLat = 0.0
        var totalLng = 0.0
        var totalWeight = 0.0

        // Dar más peso a las ubicaciones más recientes
        locations.forEachIndexed { index, location ->
            val weight = (index + 1).toDouble() // Peso creciente
            totalLat += location.latitude * weight
            totalLng += location.longitude * weight
            totalWeight += weight
        }

        val avgLocation = Location(locations.last().provider)
        avgLocation.latitude = totalLat / totalWeight
        avgLocation.longitude = totalLng / totalWeight
        avgLocation.accuracy = locations.last().accuracy
        avgLocation.time = locations.last().time

        // Mantener otros atributos de la ubicación más reciente
        val lastLocation = locations.last()
        if (lastLocation.hasAltitude()) {
            avgLocation.altitude = lastLocation.altitude
        }
        if (lastLocation.hasBearing()) {
            avgLocation.bearing = lastLocation.bearing
        }
        if (lastLocation.hasSpeed()) {
            avgLocation.speed = lastLocation.speed
        }

        return avgLocation
    }

    private fun calculateDistance(loc1: Location, loc2: Location): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            loc1.latitude, loc1.longitude,
            loc2.latitude, loc2.longitude,
            results
        )
        return results[0]
    }

    fun reset() {
        lastValidLocation = null
        lastUpdateTime = 0L
        consecutiveStationaryCount = 0
        consecutiveValidReadings = 0
        totalRejectedReadings = 0
        locationBuffer.clear()
        Log.d(TAG, "🔄 Filtro GPS reiniciado completamente")
    }

    fun getCurrentLocation(): Location? = lastValidLocation

    fun isUserMoving(): Boolean = consecutiveStationaryCount < maxStationaryCount

    fun locationToLatLng(location: Location): LatLng {
        return LatLng(location.latitude, location.longitude)
    }

    // ✅ ESTADÍSTICAS MEJORADAS
    fun getFilterStats(): String {
        return buildString {
            append("=== GPS FILTER STATS ===\n")
            append("📍 Última ubicación: ${lastValidLocation?.let { "✅" } ?: "❌"}\n")
            append("🎯 Precisión actual: ${lastValidLocation?.accuracy?.let { "${String.format("%.1f", it)}m" } ?: "N/A"}\n")
            append("🏃 Usuario en movimiento: ${if (isUserMoving()) "Sí" else "No"}\n")
            append("📊 Lecturas estacionarias: $consecutiveStationaryCount/$maxStationaryCount\n")
            append("✅ Lecturas válidas consecutivas: $consecutiveValidReadings\n")
            append("📈 Buffer de suavizado: ${locationBuffer.size}/$SMOOTHING_WINDOW_SIZE\n")
            append("❌ Total rechazos desde última aceptación: $totalRejectedReadings\n")
            append("\n=== CONFIGURACIÓN ===\n")
            append("🎯 Precisión mínima: ${MIN_ACCURACY}m\n")
            append("📏 Distancia mínima: ${MIN_DISTANCE_THRESHOLD}m\n")
            append("🏃 Velocidad máxima: ${MAX_SPEED_THRESHOLD} km/h\n")
            append("⏱️ Intervalo mínimo: ${MIN_TIME_INTERVAL/1000}s\n")
            append("🛑 Radio estacionario: ${STATIONARY_RADIUS}m\n")
            append("📍 Movimiento significativo: ${SIGNIFICANT_MOVEMENT_DISTANCE}m")
        }
    }
}

// FilterResult permanece igual
sealed class FilterResult {
    object REJECTED_LOW_ACCURACY : FilterResult()
    object REJECTED_TIME_INTERVAL : FilterResult()
    object REJECTED_SMALL_DISTANCE : FilterResult()
    object REJECTED_HIGH_SPEED : FilterResult()
    object REJECTED_STATIONARY : FilterResult()
    object ACCEPTED_FIRST_LOCATION : FilterResult()
    data class ACCEPTED_AND_SMOOTHED(val location: Location) : FilterResult()

    fun isAccepted(): Boolean = this is ACCEPTED_FIRST_LOCATION || this is ACCEPTED_AND_SMOOTHED

    fun getLocationData(): Location? = when (this) {
        is ACCEPTED_AND_SMOOTHED -> location
        else -> null
    }

    fun getResultDescription(): String = when (this) {
        is REJECTED_LOW_ACCURACY -> "❌ Rechazado: Baja precisión GPS"
        is REJECTED_TIME_INTERVAL -> "❌ Rechazado: Intervalo muy corto"
        is REJECTED_SMALL_DISTANCE -> "❌ Rechazado: Distancia insuficiente"
        is REJECTED_HIGH_SPEED -> "❌ Rechazado: Velocidad imposible"
        is REJECTED_STATIONARY -> "❌ Rechazado: Usuario estacionario"
        is ACCEPTED_FIRST_LOCATION -> "✅ Aceptado: Primera ubicación"
        is ACCEPTED_AND_SMOOTHED -> "✅ Aceptado: Ubicación válida"
    }
}