package com.example.fittrack.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fittrack.model.MetaDiaria
import com.example.fittrack.network.MetaDiariaRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MetaDiariaViewModel : ViewModel() {

    companion object {
        private const val TAG = "MetaDiariaViewModel"
    }

    private val repository = MetaDiariaRepository()

    // LiveData existente
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

    private val _estadisticasAdicionales = MutableLiveData<EstadisticasAdicionales>()
    val estadisticasAdicionales: LiveData<EstadisticasAdicionales> = _estadisticasAdicionales

    // ✨ NUEVOS LIVEDATA PARA MEJORAR LA VISUALIZACIÓN
    private val _mostrarFelicitaciones = MutableLiveData<Boolean>()
    val mostrarFelicitaciones: LiveData<Boolean> = _mostrarFelicitaciones

    private val _mostrarPuntos = MutableLiveData<Boolean>()
    val mostrarPuntos: LiveData<Boolean> = _mostrarPuntos

    private val _mostrarCompartir = MutableLiveData<Boolean>()
    val mostrarCompartir: LiveData<Boolean> = _mostrarCompartir

    private val _mensajeFelicitaciones = MutableLiveData<String>()
    val mensajeFelicitaciones: LiveData<String> = _mensajeFelicitaciones

    private val _tipoLogro = MutableLiveData<TipoLogro>()
    val tipoLogro: LiveData<TipoLogro> = _tipoLogro

    private val _datosParaCompartir = MutableLiveData<DatosCompartir>()
    val datosParaCompartir: LiveData<DatosCompartir> = _datosParaCompartir

    // ✨ NUEVO: LiveData para actualización automática
    private val _progresoActualizadoAutomaticamente = MutableLiveData<Boolean>()
    val progresoActualizadoAutomaticamente: LiveData<Boolean> = _progresoActualizadoAutomaticamente

    private var currentUserId: String? = null
    private var isAutoRefreshEnabled = true  // ✨ Control de refresco automático

    fun loadGoalData(userId: String?) {
        if (userId.isNullOrEmpty()) {
            _errorMessage.value = "ID de usuario no válido"
            return
        }

        currentUserId = userId
        viewModelScope.launch {
            try {
                Log.d(TAG, "Cargando datos de meta diaria para: $userId")
                _isLoading.value = true

                val resultMeta = repository.getMetaDiariaActual(userId)

                if (resultMeta.isSuccess) {
                    val meta = resultMeta.getOrNull()!!

                    actualizarDatosBasicos(meta)
                    actualizarVisualizacionDinamica(meta)
                    loadEstadisticas(userId)

                    // ✨ INICIAR MONITOREO AUTOMÁTICO
                    if (isAutoRefreshEnabled) {
                        iniciarMonitoreoAutomatico()
                    }

                    Log.d(TAG, "Datos de meta diaria cargados exitosamente")
                } else {
                    val error = resultMeta.exceptionOrNull()
                    Log.e(TAG, "Error al cargar meta: ${error?.message}", error)
                    _errorMessage.value = "Error al cargar datos de meta diaria"
                }

                _isLoading.value = false

            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar datos: ${e.message}", e)
                _errorMessage.value = "Error al cargar datos de meta diaria"
                _isLoading.value = false
            }
        }
    }

    // ✨ NUEVA FUNCIÓN: Monitoreo automático del progreso
    private fun iniciarMonitoreoAutomatico() {
        viewModelScope.launch {
            try {
                while (isAutoRefreshEnabled && currentUserId != null) {
                    delay(30000) // Verificar cada 30 segundos

                    if (isAutoRefreshEnabled && currentUserId != null) {
                        verificarActualizacionProgreso()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en monitoreo automático: ${e.message}", e)
            }
        }
    }

    // ✨ NUEVA FUNCIÓN: Verificar si hay actualizaciones en el progreso
    private suspend fun verificarActualizacionProgreso() {
        try {
            val userId = currentUserId ?: return
            val progresoAnterior = _progresoActual.value ?: 0.0

            // Obtener progreso actual desde la BD
            val resultadoProgreso = repository.getProgresoTiempoReal(userId)

            if (resultadoProgreso.isSuccess) {
                val progresoNuevo = resultadoProgreso.getOrNull() ?: 0.0

                // Solo actualizar si hay cambios significativos (diferencia > 0.01 km)
                if (kotlin.math.abs(progresoNuevo - progresoAnterior) > 0.01) {
                    Log.d(TAG, "🔄 Progreso actualizado automáticamente: $progresoAnterior → $progresoNuevo km")

                    // Recargar datos completos
                    val resultMeta = repository.getMetaDiariaActual(userId)
                    if (resultMeta.isSuccess) {
                        val metaActualizada = resultMeta.getOrNull()!!

                        actualizarDatosBasicos(metaActualizada)
                        actualizarVisualizacionDinamica(metaActualizada)

                        // Notificar que se actualizó automáticamente
                        _progresoActualizadoAutomaticamente.value = true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar actualización: ${e.message}", e)
        }
    }

    // ✨ NUEVA FUNCIÓN: Forzar actualización inmediata
    fun actualizarProgresoInmediatamente() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🚀 Forzando actualización inmediata del progreso")
                verificarActualizacionProgreso()
            } catch (e: Exception) {
                Log.e(TAG, "Error en actualización inmediata: ${e.message}", e)
            }
        }
    }

    // ✨ NUEVA FUNCIÓN: Activar/desactivar monitoreo automático
    fun setAutoRefresh(enabled: Boolean) {
        isAutoRefreshEnabled = enabled
        Log.d(TAG, "Monitoreo automático ${if (enabled) "activado" else "desactivado"}")

        if (enabled && currentUserId != null) {
            iniciarMonitoreoAutomatico()
        }
    }

    private fun actualizarDatosBasicos(meta: MetaDiaria) {
        Log.d(TAG, "=== ACTUALIZANDO DATOS BÁSICOS ===")
        Log.d(TAG, "Progreso: ${meta.progresoActual}")
        Log.d(TAG, "Meta: ${meta.metaKilometros}")
        Log.d(TAG, "Porcentaje: ${meta.porcentajeCompletado}")
        Log.d(TAG, "Puntos: ${meta.puntosGanados}")

        _progresoActual.value = meta.progresoActual
        _metaDiaria.value = meta.metaKilometros
        _porcentajeCompletado.value = meta.porcentajeCompletado
        _puntosGanados.value = meta.puntosGanados

        val mensaje = generarMensajeMeta(meta.porcentajeCompletado)
        _mensajeMeta.value = mensaje

        Log.d(TAG, "Mensaje generado: $mensaje")
    }

    private fun actualizarVisualizacionDinamica(meta: MetaDiaria) {
        val porcentaje = meta.porcentajeCompletado
        Log.d(TAG, "=== ACTUALIZANDO VISUALIZACIÓN DINÁMICA ===")
        Log.d(TAG, "Porcentaje para lógica: $porcentaje")

        // Determinar qué mostrar basado en el progreso
        when {
            porcentaje >= 100 -> {
                Log.d(TAG, "Estado: META_COMPLETADA (≥100%)")
                _mostrarFelicitaciones.value = true
                _mostrarPuntos.value = true
                _mostrarCompartir.value = true
                _mensajeFelicitaciones.value = "¡Impresionante! Tu dedicación es inspiradora 🏆"
                _tipoLogro.value = TipoLogro.META_COMPLETADA
            }
            porcentaje >= 75 -> {
                Log.d(TAG, "Estado: CASI_COMPLETA (75-99%)")
                _mostrarFelicitaciones.value = true
                _mostrarPuntos.value = true
                _mostrarCompartir.value = false
                _mensajeFelicitaciones.value = "¡Excelente progreso! Estás muy cerca 💪"
                _tipoLogro.value = TipoLogro.CASI_COMPLETA
            }
            porcentaje >= 50 -> {
                Log.d(TAG, "Estado: MEDIO_CAMINO (50-74%)")
                _mostrarFelicitaciones.value = true
                _mostrarPuntos.value = true
                _mostrarCompartir.value = false
                _mensajeFelicitaciones.value = "¡Vas por buen camino! Sigue así 🚀"
                _tipoLogro.value = TipoLogro.MEDIO_CAMINO
            }
            porcentaje >= 25 -> {
                Log.d(TAG, "Estado: INICIO (25-49%)")
                _mostrarFelicitaciones.value = false
                _mostrarPuntos.value = true
                _mostrarCompartir.value = false
                _tipoLogro.value = TipoLogro.INICIO
            }
            else -> {
                Log.d(TAG, "Estado: SIN_PROGRESO (0-24%)")
                _mostrarFelicitaciones.value = false
                _mostrarPuntos.value = false
                _mostrarCompartir.value = false
                _tipoLogro.value = TipoLogro.SIN_PROGRESO
            }
        }

        Log.d(TAG, "Cards visibles - Felicitaciones: ${_mostrarFelicitaciones.value}, Puntos: ${_mostrarPuntos.value}, Compartir: ${_mostrarCompartir.value}")

        // Preparar datos para compartir
        prepararDatosParaCompartir(meta)
    }

    private fun prepararDatosParaCompartir(meta: MetaDiaria) {
        val datos = DatosCompartir(
            progreso = meta.progresoActual,
            meta = meta.metaKilometros,
            porcentaje = meta.porcentajeCompletado,
            puntos = meta.puntosGanados,
            fecha = meta.fecha,
            mensajePersonalizado = generarMensajeParaCompartir(meta)
        )
        _datosParaCompartir.value = datos
    }

    private fun generarMensajeParaCompartir(meta: MetaDiaria): String {
        return when {
            meta.porcentajeCompletado >= 100 ->
                "🏆 ¡Meta diaria completada! Recorrí ${String.format("%.1f", meta.progresoActual)} km de ${String.format("%.1f", meta.metaKilometros)} km. ¡${meta.puntosGanados} puntos ganados! 💪 #FitTrack #MetaCumplida"

            meta.porcentajeCompletado >= 75 ->
                "🚀 ¡${meta.porcentajeCompletado}% de mi meta diaria! Recorrí ${String.format("%.1f", meta.progresoActual)} km de ${String.format("%.1f", meta.metaKilometros)} km. ¡Casi lo logro! 💪 #FitTrack #CasiAhi"

            meta.porcentajeCompletado >= 50 ->
                "💪 ¡A mitad de camino! ${String.format("%.1f", meta.progresoActual)} km de ${String.format("%.1f", meta.metaKilometros)} km completados (${meta.porcentajeCompletado}%). ¡Sigo adelante! #FitTrack #MitadCamino"

            else ->
                "🚀 ¡Comenzando mi día activo! Meta de hoy: ${String.format("%.1f", meta.metaKilometros)} km. ¡Vamos por ello! #FitTrack #VidaActiva"
        }
    }

    private suspend fun loadEstadisticas(userId: String) {
        try {
            val resultEstadisticas = repository.getEstadisticasUsuario(userId)

            if (resultEstadisticas.isSuccess) {
                val stats = resultEstadisticas.getOrNull()!!

                val estadisticas = EstadisticasAdicionales(
                    diasConsecutivos = stats.diasConsecutivos,
                    metasAlcanzadas = stats.metasAlcanzadas,
                    mejorRacha = stats.mejorRacha,
                    promedioSemanal = stats.promedioSemanal
                )
                _estadisticasAdicionales.value = estadisticas
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar estadísticas: ${e.message}", e)
        }
    }

    fun updateGoal(nuevaMeta: Double) {
        val userId = currentUserId
        if (userId.isNullOrEmpty()) {
            _errorMessage.value = "Usuario no identificado"
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Actualizando meta diaria a: $nuevaMeta km")
                _isLoading.value = true

                val fechaHoy = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date())

                val result = repository.actualizarMeta(userId, fechaHoy, nuevaMeta)

                if (result.isSuccess) {
                    val metaActualizada = result.getOrNull()!!

                    actualizarDatosBasicos(metaActualizada)
                    actualizarVisualizacionDinamica(metaActualizada)

                    _metaActualizada.value = true

                    Log.d(TAG, "Meta actualizada exitosamente")
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "Error al actualizar meta: ${error?.message}", error)
                    _errorMessage.value = "Error al actualizar la meta"
                }

                _isLoading.value = false

            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar meta: ${e.message}", e)
                _errorMessage.value = "Error al actualizar la meta"
                _isLoading.value = false
            }
        }
    }

    fun refreshData() {
        currentUserId?.let { userId ->
            loadGoalData(userId)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearMetaActualizadaFlag() {
        _metaActualizada.value = false
    }

    fun clearProgresoActualizadoFlag() {
        _progresoActualizadoAutomaticamente.value = false
    }

    fun getCurrentGoal(): Double {
        return _metaDiaria.value ?: 10.0
    }

    fun getCurrentProgress(): Double {
        return _progresoActual.value ?: 0.0
    }

    fun getCurrentPercentage(): Int {
        return _porcentajeCompletado.value ?: 0
    }

    private fun generarMensajeMeta(porcentaje: Int): String {
        return when {
            porcentaje >= 100 -> "¡Meta alcanzada!"
            porcentaje >= 75 -> "¡Casi lo logras!"
            porcentaje >= 50 -> "¡Vas por buen camino!"
            porcentaje >= 25 -> "¡Sigue así!"
            else -> "¡Vamos, tú puedes!"
        }
    }

    override fun onCleared() {
        super.onCleared()
        isAutoRefreshEnabled = false
        Log.d(TAG, "ViewModel cleared - Monitoreo automático detenido")
    }

    // Data classes
    data class EstadisticasAdicionales(
        val diasConsecutivos: Int,
        val metasAlcanzadas: Int,
        val mejorRacha: Int,
        val promedioSemanal: Double
    )

    data class DatosCompartir(
        val progreso: Double,
        val meta: Double,
        val porcentaje: Int,
        val puntos: Int,
        val fecha: String,
        val mensajePersonalizado: String
    )

    enum class TipoLogro {
        SIN_PROGRESO,
        INICIO,
        MEDIO_CAMINO,
        CASI_COMPLETA,
        META_COMPLETADA
    }
}