package com.example.fittrack.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MetaDiariaViewModel : ViewModel() {

    companion object {
        private const val TAG = "MetaDiariaViewModel"
    }

    // LiveData para los datos de meta diaria
    private val _progresoActual = MutableLiveData<Double>()
    val progresoActual: LiveData<Double> = _progresoActual

    private val _metaDiaria = MutableLiveData<Double>()
    val metaDiaria: LiveData<Double> = _metaDiaria

    private val _porcentajeCompletado = MutableLiveData<Int>()
    val porcentajeCompletado: LiveData<Int> = _porcentajeCompletado

    private val _mensajeMeta = MutableLiveData<String>()
    val mensajeMeta: LiveData<String> = _mensajeMeta

    private val _puntosGanados = MutableLiveData<Int>()
    val puntosGanados: LiveData<Int> = _puntosGanados

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _metaActualizada = MutableLiveData<Boolean>()
    val metaActualizada: LiveData<Boolean> = _metaActualizada

    // Datos adicionales para la UI
    private val _estadisticasAdicionales = MutableLiveData<EstadisticasAdicionales>()
    val estadisticasAdicionales: LiveData<EstadisticasAdicionales> = _estadisticasAdicionales

    fun loadGoalData(userName: String?) {
        try {
            Log.d(TAG, "Cargando datos de meta diaria para: $userName")

            _isLoading.value = true

            // Simular carga de datos
            loadMockGoalData()

            _isLoading.value = false

        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar datos: ${e.message}", e)
            _errorMessage.value = "Error al cargar datos de meta diaria"
            _isLoading.value = false
        }
    }

    private fun loadMockGoalData() {
        try {
            // Datos principales de la meta
            val progreso = 10.0
            val meta = 10.0

            _progresoActual.value = progreso
            _metaDiaria.value = meta

            // Calcular porcentaje
            val porcentaje = if (meta > 0) {
                ((progreso / meta) * 100).toInt().coerceAtMost(100)
            } else {
                0
            }
            _porcentajeCompletado.value = porcentaje

            // Mensaje según el progreso
            val mensaje = when {
                porcentaje >= 100 -> "¡Meta alcanzada!"
                porcentaje >= 75 -> "¡Casi lo logras!"
                porcentaje >= 50 -> "¡Vas por buen camino!"
                porcentaje >= 25 -> "¡Sigue así!"
                else -> "¡Vamos, tú puedes!"
            }
            _mensajeMeta.value = mensaje

            // Puntos ganados (ejemplo de cálculo)
            val puntos = when {
                porcentaje >= 100 -> 50
                porcentaje >= 75 -> 35
                porcentaje >= 50 -> 25
                porcentaje >= 25 -> 15
                else -> 5
            }
            _puntosGanados.value = puntos

            // Estadísticas adicionales
            val estadisticas = EstadisticasAdicionales(
                diasConsecutivos = 7,
                metasAlcanzadas = 15,
                mejorRacha = 12,
                promedioSemanal = 8.5
            )
            _estadisticasAdicionales.value = estadisticas

            Log.d(TAG, "Datos mock de meta diaria cargados exitosamente")

        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar datos mock: ${e.message}", e)
            _errorMessage.value = "Error al procesar los datos"
        }
    }

    fun updateGoal(nuevaMeta: Double) {
        try {
            Log.d(TAG, "Actualizando meta diaria a: $nuevaMeta km")

            _isLoading.value = true

            // Simular guardado (aquí harías la llamada a tu API/BD)
            _metaDiaria.value = nuevaMeta

            // Recalcular porcentaje con la nueva meta
            val progresoActual = _progresoActual.value ?: 0.0
            val porcentaje = if (nuevaMeta > 0) {
                ((progresoActual / nuevaMeta) * 100).toInt().coerceAtMost(100)
            } else {
                0
            }
            _porcentajeCompletado.value = porcentaje

            // Actualizar mensaje
            val mensaje = when {
                porcentaje >= 100 -> "¡Meta alcanzada!"
                porcentaje >= 75 -> "¡Casi lo logras!"
                porcentaje >= 50 -> "¡Vas por buen camino!"
                porcentaje >= 25 -> "¡Sigue así!"
                else -> "¡Vamos, tú puedes!"
            }
            _mensajeMeta.value = mensaje

            // Actualizar puntos
            val puntos = when {
                porcentaje >= 100 -> 50
                porcentaje >= 75 -> 35
                porcentaje >= 50 -> 25
                porcentaje >= 25 -> 15
                else -> 5
            }
            _puntosGanados.value = puntos

            _metaActualizada.value = true
            _isLoading.value = false

            Log.d(TAG, "Meta actualizada exitosamente")

        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar meta: ${e.message}", e)
            _errorMessage.value = "Error al actualizar la meta"
            _isLoading.value = false
        }
    }

    fun updateProgress(nuevoProgreso: Double) {
        try {
            Log.d(TAG, "Actualizando progreso a: $nuevoProgreso km")

            _progresoActual.value = nuevoProgreso

            // Recalcular porcentaje
            val meta = _metaDiaria.value ?: 10.0
            val porcentaje = if (meta > 0) {
                ((nuevoProgreso / meta) * 100).toInt().coerceAtMost(100)
            } else {
                0
            }
            _porcentajeCompletado.value = porcentaje

            // Actualizar mensaje
            val mensaje = when {
                porcentaje >= 100 -> "¡Meta alcanzada!"
                porcentaje >= 75 -> "¡Casi lo logras!"
                porcentaje >= 50 -> "¡Vas por buen camino!"
                porcentaje >= 25 -> "¡Sigue así!"
                else -> "¡Vamos, tú puedes!"
            }
            _mensajeMeta.value = mensaje

            // Actualizar puntos
            val puntos = when {
                porcentaje >= 100 -> 50
                porcentaje >= 75 -> 35
                porcentaje >= 50 -> 25
                porcentaje >= 25 -> 15
                else -> 5
            }
            _puntosGanados.value = puntos

            Log.d(TAG, "Progreso actualizado exitosamente")

        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar progreso: ${e.message}", e)
            _errorMessage.value = "Error al actualizar el progreso"
        }
    }

    fun refreshData() {
        loadGoalData(null)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearMetaActualizadaFlag() {
        _metaActualizada.value = false
    }

    // Métodos para obtener valores actuales (útil para validaciones)
    fun getCurrentGoal(): Double {
        return _metaDiaria.value ?: 10.0
    }

    fun getCurrentProgress(): Double {
        return _progresoActual.value ?: 0.0
    }

    fun getCurrentPercentage(): Int {
        return _porcentajeCompletado.value ?: 0
    }

    // Data class para estadísticas adicionales
    data class EstadisticasAdicionales(
        val diasConsecutivos: Int,
        val metasAlcanzadas: Int,
        val mejorRacha: Int,
        val promedioSemanal: Double
    )
}