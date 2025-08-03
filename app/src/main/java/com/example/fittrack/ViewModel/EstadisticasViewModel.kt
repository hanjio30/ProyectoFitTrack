package com.example.fittrack.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fittrack.network.RecorridoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EstadisticasViewModel : ViewModel() {

    private val recorridoRepository = RecorridoRepository()
    private val TAG = "EstadisticasViewModel"

    // Estados para las estadísticas
    private val _estadisticas = MutableStateFlow(EstadisticasUiState())
    val estadisticas: StateFlow<EstadisticasUiState> = _estadisticas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Data class para el estado de la UI
    data class EstadisticasUiState(
        val totalPasos: Int = 0,
        val totalCalorias: Int = 0,
        val totalMinutos: Int = 0,
        val totalDistanciaKm: Double = 0.0,
        val totalRecorridos: Int = 0,
        val fechaActual: String = ""
    )

    init {
        // Cargar estadísticas del día actual al inicializar
        cargarEstadisticasDelDia()
    }

    /**
     * Cargar estadísticas del día actual
     */
    fun cargarEstadisticasDelDia(userId: String? = null) {
        if (userId.isNullOrEmpty()) {
            Log.w(TAG, "userId es null o vacío, no se pueden cargar estadísticas")
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                Log.d(TAG, "Cargando estadísticas del día para usuario: $userId")

                val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val fechaFormateada = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())

                val resultado = recorridoRepository.obtenerEstadisticasDiarias(userId, fechaHoy)

                if (resultado.isSuccess) {
                    val estadisticas = resultado.getOrNull()!!

                    _estadisticas.value = EstadisticasUiState(
                        totalPasos = estadisticas.totalPasos,
                        totalCalorias = estadisticas.totalCalorias,
                        totalMinutos = estadisticas.totalMinutos,
                        totalDistanciaKm = estadisticas.totalDistanciaKm,
                        totalRecorridos = estadisticas.totalRecorridos,
                        fechaActual = fechaFormateada
                    )

                    Log.d(TAG, "✅ Estadísticas cargadas exitosamente:")
                    Log.d(TAG, "   Pasos: ${estadisticas.totalPasos}")
                    Log.d(TAG, "   Calorías: ${estadisticas.totalCalorias}")
                    Log.d(TAG, "   Minutos: ${estadisticas.totalMinutos}")
                    Log.d(TAG, "   Distancia: ${estadisticas.totalDistanciaKm} km")
                    Log.d(TAG, "   Recorridos: ${estadisticas.totalRecorridos}")

                } else {
                    val exception = resultado.exceptionOrNull()
                    Log.e(TAG, "Error al cargar estadísticas: ${exception?.message}", exception)
                    _error.value = "Error al cargar estadísticas: ${exception?.message}"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado al cargar estadísticas: ${e.message}", e)
                _error.value = "Error inesperado: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cargar estadísticas para una fecha específica
     */
    fun cargarEstadisticasPorFecha(userId: String, fecha: String) {
        if (userId.isEmpty()) {
            Log.w(TAG, "userId está vacío, no se pueden cargar estadísticas")
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                Log.d(TAG, "Cargando estadísticas para fecha: $fecha, usuario: $userId")

                val resultado = recorridoRepository.obtenerEstadisticasDiarias(userId, fecha)

                if (resultado.isSuccess) {
                    val estadisticas = resultado.getOrNull()!!
                    val fechaFormateada = try {
                        val fechaOriginal = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(fecha)
                        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(fechaOriginal ?: Date())
                    } catch (e: Exception) {
                        fecha
                    }

                    _estadisticas.value = EstadisticasUiState(
                        totalPasos = estadisticas.totalPasos,
                        totalCalorias = estadisticas.totalCalorias,
                        totalMinutos = estadisticas.totalMinutos,
                        totalDistanciaKm = estadisticas.totalDistanciaKm,
                        totalRecorridos = estadisticas.totalRecorridos,
                        fechaActual = fechaFormateada
                    )

                    Log.d(TAG, "✅ Estadísticas cargadas para $fecha")

                } else {
                    val exception = resultado.exceptionOrNull()
                    Log.e(TAG, "Error al cargar estadísticas para $fecha: ${exception?.message}", exception)
                    _error.value = "Error al cargar estadísticas: ${exception?.message}"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado al cargar estadísticas para $fecha: ${e.message}", e)
                _error.value = "Error inesperado: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Refrescar estadísticas
     */
    fun refrescarEstadisticas(userId: String) {
        Log.d(TAG, "Refrescando estadísticas para usuario: $userId")
        cargarEstadisticasDelDia(userId)
    }

    /**
     * Limpiar error
     */
    fun limpiarError() {
        _error.value = null
    }
}