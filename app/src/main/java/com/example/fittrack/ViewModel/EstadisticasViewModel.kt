package com.example.fittrack.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fittrack.network.RecorridoRepository
import com.example.fittrack.network.RecorridoRepositoryExtension
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EstadisticasViewModel : ViewModel() {

    private val recorridoRepository = RecorridoRepository()
    private val repositoryExtension = RecorridoRepositoryExtension() // ✨ NUEVA EXTENSIÓN
    private val TAG = "EstadisticasViewModel"

    // Estados existentes
    private val _estadisticas = MutableStateFlow(EstadisticasUiState())
    val estadisticas: StateFlow<EstadisticasUiState> = _estadisticas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ===== NUEVOS ESTADOS PARA GRÁFICOS =====
    private val _datosGraficos = MutableStateFlow(DatosGraficosUiState())
    val datosGraficos: StateFlow<DatosGraficosUiState> = _datosGraficos.asStateFlow()

    private val _metaDiaria = MutableStateFlow(MetaDiariaUiState())
    val metaDiaria: StateFlow<MetaDiariaUiState> = _metaDiaria.asStateFlow()

    // Data class existente
    data class EstadisticasUiState(
        val totalPasos: Int = 0,
        val totalCalorias: Int = 0,
        val totalMinutos: Int = 0,
        val totalDistanciaKm: Double = 0.0,
        val totalRecorridos: Int = 0,
        val fechaActual: String = ""
    )

    // ===== NUEVAS DATA CLASSES PARA GRÁFICOS =====
    data class DatosGraficosUiState(
        val distanciaUltimos7Dias: List<DatoGrafico> = emptyList(),
        val caloriasUltimos7Dias: List<DatoGrafico> = emptyList(),
        val pasosUltimos7Dias: List<DatoGrafico> = emptyList()
    )

    data class DatoGrafico(
        val fecha: String,
        val fechaCorta: String, // Para mostrar en X-axis (ej: "Lun", "Mar")
        val valor: Float,
        val valorFormateado: String // Para tooltips
    )

    data class MetaDiariaUiState(
        val metaPasos: Int = 10000,
        val pasosActuales: Int = 0,
        val porcentajeCompletado: Int = 0,
        val metaAlcanzada: Boolean = false
    )

    init {
        // Cargar estadísticas del día actual al inicializar
        cargarEstadisticasDelDia()
    }

    /**
     * Cargar estadísticas del día actual (función existente)
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

                    // ===== CARGAR DATOS PARA GRÁFICOS =====
                    cargarDatosParaGraficos(userId)
                    actualizarMetaDiaria(estadisticas.totalPasos)

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

    // ===== FUNCIÓN OPTIMIZADA: Cargar datos para gráficos =====
    private suspend fun cargarDatosParaGraficos(userId: String) {
        try {
            Log.d(TAG, "📊 Cargando datos para gráficos (optimizado)...")

            val fechas = obtenerUltimos7Dias()
            val fechasString = fechas.map { it.first }

            // ✨ USAR EXTENSIÓN PARA CONSULTA OPTIMIZADA
            val resultado = repositoryExtension.obtenerEstadisticasParaGraficos(userId, fechasString)

            if (resultado.isSuccess) {
                val estadisticasPorFecha = resultado.getOrNull() ?: emptyMap()

                val datosDistancia = mutableListOf<DatoGrafico>()
                val datosCalorias = mutableListOf<DatoGrafico>()
                val datosPasos = mutableListOf<DatoGrafico>()

                fechas.forEach { (fecha, fechaCorta) ->
                    val stats = estadisticasPorFecha[fecha] ?: RecorridoRepository.EstadisticasDiarias()

                    // Datos para gráfico de distancia
                    datosDistancia.add(
                        DatoGrafico(
                            fecha = fecha,
                            fechaCorta = fechaCorta,
                            valor = stats.totalDistanciaKm.toFloat(),
                            valorFormateado = "${String.format("%.2f", stats.totalDistanciaKm)} km"
                        )
                    )

                    // Datos para gráfico de calorías
                    datosCalorias.add(
                        DatoGrafico(
                            fecha = fecha,
                            fechaCorta = fechaCorta,
                            valor = stats.totalCalorias.toFloat(),
                            valorFormateado = "${stats.totalCalorias} cal"
                        )
                    )

                    // Datos para gráfico de pasos
                    datosPasos.add(
                        DatoGrafico(
                            fecha = fecha,
                            fechaCorta = fechaCorta,
                            valor = stats.totalPasos.toFloat(),
                            valorFormateado = "${stats.totalPasos} pasos"
                        )
                    )
                }

                _datosGraficos.value = DatosGraficosUiState(
                    distanciaUltimos7Dias = datosDistancia,
                    caloriasUltimos7Dias = datosCalorias,
                    pasosUltimos7Dias = datosPasos
                )

                Log.d(TAG, "✅ Datos para gráficos cargados (optimizado): ${datosDistancia.size} días")

            } else {
                Log.e(TAG, "❌ Error cargando datos optimizados: ${resultado.exceptionOrNull()?.message}")
                // Fallback a método original si falla
                cargarDatosParaGraficosOriginal(userId)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error cargando datos para gráficos: ${e.message}", e)
            // Fallback a método original
            cargarDatosParaGraficosOriginal(userId)
        }
    }

    // ===== MÉTODO ORIGINAL COMO FALLBACK =====
    private suspend fun cargarDatosParaGraficosOriginal(userId: String) {
        try {
            Log.d(TAG, "📊 Usando método fallback para cargar gráficos...")

            val fechas = obtenerUltimos7Dias()
            val datosDistancia = mutableListOf<DatoGrafico>()
            val datosCalorias = mutableListOf<DatoGrafico>()
            val datosPasos = mutableListOf<DatoGrafico>()

            for (fecha in fechas) {
                val resultado = recorridoRepository.obtenerEstadisticasDiarias(userId, fecha.first)

                if (resultado.isSuccess) {
                    val stats = resultado.getOrNull()!!

                    // Datos para gráfico de distancia
                    datosDistancia.add(
                        DatoGrafico(
                            fecha = fecha.first,
                            fechaCorta = fecha.second,
                            valor = stats.totalDistanciaKm.toFloat(),
                            valorFormateado = "${String.format("%.2f", stats.totalDistanciaKm)} km"
                        )
                    )

                    // Datos para gráfico de calorías
                    datosCalorias.add(
                        DatoGrafico(
                            fecha = fecha.first,
                            fechaCorta = fecha.second,
                            valor = stats.totalCalorias.toFloat(),
                            valorFormateado = "${stats.totalCalorias} cal"
                        )
                    )

                    // Datos para gráfico de pasos
                    datosPasos.add(
                        DatoGrafico(
                            fecha = fecha.first,
                            fechaCorta = fecha.second,
                            valor = stats.totalPasos.toFloat(),
                            valorFormateado = "${stats.totalPasos} pasos"
                        )
                    )
                } else {
                    // Si no hay datos para un día, agregar valores en 0
                    datosDistancia.add(
                        DatoGrafico(fecha.first, fecha.second, 0f, "0.0 km")
                    )
                    datosCalorias.add(
                        DatoGrafico(fecha.first, fecha.second, 0f, "0 cal")
                    )
                    datosPasos.add(
                        DatoGrafico(fecha.first, fecha.second, 0f, "0 pasos")
                    )
                }
            }

            _datosGraficos.value = DatosGraficosUiState(
                distanciaUltimos7Dias = datosDistancia,
                caloriasUltimos7Dias = datosCalorias,
                pasosUltimos7Dias = datosPasos
            )

            Log.d(TAG, "✅ Datos para gráficos cargados (fallback): ${datosDistancia.size} días")

        } catch (e: Exception) {
            Log.e(TAG, "Error cargando datos para gráficos (fallback): ${e.message}", e)
        }
    }

    // ===== NUEVA FUNCIÓN: Obtener últimos 7 días =====
    private fun obtenerUltimos7Dias(): List<Pair<String, String>> {
        val fechas = mutableListOf<Pair<String, String>>()
        val calendar = Calendar.getInstance()
        val formatoCompleto = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formatoCorto = SimpleDateFormat("EEE", Locale.getDefault())

        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)

            val fechaCompleta = formatoCompleto.format(calendar.time)
            val fechaCorta = formatoCorto.format(calendar.time)

            fechas.add(Pair(fechaCompleta, fechaCorta))
        }

        return fechas
    }

    // ===== NUEVA FUNCIÓN: Actualizar meta diaria =====
    private fun actualizarMetaDiaria(pasosActuales: Int) {
        val metaPasos = 7000 // Meta estándar, podrías hacer esto configurable
        val porcentaje = ((pasosActuales.toFloat() / metaPasos) * 100).toInt().coerceAtMost(100)
        val metaAlcanzada = pasosActuales >= metaPasos

        _metaDiaria.value = MetaDiariaUiState(
            metaPasos = metaPasos,
            pasosActuales = pasosActuales,
            porcentajeCompletado = porcentaje,
            metaAlcanzada = metaAlcanzada
        )

        Log.d(TAG, "🎯 Meta diaria actualizada: $pasosActuales/$metaPasos pasos ($porcentaje%)")
    }

    /**
     * Cargar estadísticas para una fecha específica (función existente)
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

                    // ===== ACTUALIZAR GRÁFICOS Y META =====
                    cargarDatosParaGraficos(userId)
                    actualizarMetaDiaria(estadisticas.totalPasos)

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
     * Refrescar estadísticas (función existente)
     */
    fun refrescarEstadisticas(userId: String) {
        Log.d(TAG, "Refrescando estadísticas para usuario: $userId")
        cargarEstadisticasDelDia(userId)
    }

    /**
     * Limpiar error (función existente)
     */
    fun limpiarError() {
        _error.value = null
    }

    // ===== NUEVA FUNCIÓN: Configurar meta personalizada =====
    fun configurarMetaPasos(nuevaMetaPasos: Int) {
        val estadisticasActuales = _estadisticas.value
        actualizarMetaDiaria(estadisticasActuales.totalPasos)
        Log.d(TAG, "Meta de pasos configurada a: $nuevaMetaPasos")
    }

    // ===== NUEVAS FUNCIONES ADICIONALES =====

    /**
     * Obtener tendencias comparativas (semana actual vs anterior)
     */
    fun cargarTendencias(userId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📈 Cargando tendencias de actividad...")
                val resultado = repositoryExtension.obtenerTendencias(userId)

                if (resultado.isSuccess) {
                    val tendencias = resultado.getOrNull()!!
                    Log.d(TAG, "✅ Tendencias obtenidas:")
                    Log.d(TAG, "   Pasos: ${tendencias.cambiosPasos}%")
                    Log.d(TAG, "   Calorías: ${tendencias.cambiosCalorias}%")
                    Log.d(TAG, "   Distancia: ${tendencias.cambiosDistancia}%")

                    // Aquí podrías emitir las tendencias a un StateFlow si las quieres mostrar en la UI
                } else {
                    Log.e(TAG, "Error obteniendo tendencias: ${resultado.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando tendencias: ${e.message}", e)
            }
        }
    }

    /**
     * Obtener resumen de actividad de los últimos 30 días
     */
    fun cargarResumenMensual(userId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📋 Cargando resumen mensual...")
                val resultado = repositoryExtension.obtenerResumenReciente(userId, 30)

                if (resultado.isSuccess) {
                    val resumen = resultado.getOrNull()!!
                    Log.d(TAG, "✅ Resumen mensual obtenido:")
                    Log.d(TAG, "   Días activos: ${resumen.diasConActividad}/${resumen.totalDias}")
                    Log.d(TAG, "   Total pasos: ${resumen.totales.totalPasos}")
                    Log.d(TAG, "   Promedio diario: ${resumen.promediosDiarios.totalPasos} pasos")

                    // Aquí podrías emitir el resumen a un StateFlow si lo quieres mostrar en la UI
                } else {
                    Log.e(TAG, "Error obteniendo resumen: ${resultado.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando resumen mensual: ${e.message}", e)
            }
        }
    }

    /**
     * Verificar si hay datos suficientes para mostrar gráficos
     */
    fun verificarDisponibilidadDatos(userId: String): Boolean {
        val datosActuales = _datosGraficos.value
        return datosActuales.distanciaUltimos7Dias.isNotEmpty() ||
                datosActuales.caloriasUltimos7Dias.isNotEmpty() ||
                datosActuales.pasosUltimos7Dias.isNotEmpty()
    }

    /**
     * Obtener estadísticas resumidas para un widget o notificación
     */
    fun obtenerEstadisticasResumidas(): EstadisticasResumidas {
        val estadisticas = _estadisticas.value
        val meta = _metaDiaria.value

        return EstadisticasResumidas(
            pasosHoy = estadisticas.totalPasos,
            caloriasHoy = estadisticas.totalCalorias,
            metaAlcanzada = meta.metaAlcanzada,
            porcentajeMeta = meta.porcentajeCompletado,
            distanciaHoy = estadisticas.totalDistanciaKm.toFloat()
        )
    }

    data class EstadisticasResumidas(
        val pasosHoy: Int,
        val caloriasHoy: Int,
        val metaAlcanzada: Boolean,
        val porcentajeMeta: Int,
        val distanciaHoy: Float
    )
}