package com.example.fittrack.network

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Extensión del RecorridoRepository para funciones específicas de gráficos
 * Esta clase se puede integrar directamente en RecorridoRepository.kt
 */
class RecorridoRepositoryExtension {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "RecorridoRepoExtension"

    /**
     * Obtener estadísticas para múltiples fechas de manera eficiente
     * Esta función optimiza las consultas para evitar múltiples llamadas a Firebase
     */
    suspend fun obtenerEstadisticasParaGraficos(
        userId: String,
        fechas: List<String>
    ): Result<Map<String, RecorridoRepository.EstadisticasDiarias>> {
        return try {
            Log.d(TAG, "📊 Obteniendo estadísticas para ${fechas.size} fechas")

            val fechaInicio = fechas.minOrNull() ?: return Result.success(emptyMap())
            val fechaFin = fechas.maxOrNull() ?: return Result.success(emptyMap())

            // Obtener todos los recorridos en el rango de fechas con una sola consulta
            val documents = db.collection("users")
                .document(userId)
                .collection("recorridos")
                .whereGreaterThanOrEqualTo("fecha", fechaInicio)
                .whereLessThanOrEqualTo("fecha", fechaFin)
                .get()
                .await()

            // Agrupar por fecha
            val estadisticasPorFecha = mutableMapOf<String, RecorridoRepository.EstadisticasDiarias>()

            // Inicializar todas las fechas con valores en 0
            fechas.forEach { fecha ->
                estadisticasPorFecha[fecha] = RecorridoRepository.EstadisticasDiarias()
            }

            // Agrupar documentos por fecha
            val recorridosPorFecha = documents.documents.groupBy { doc ->
                doc.getString("fecha") ?: ""
            }

            // Calcular estadísticas para cada fecha
            recorridosPorFecha.forEach { (fecha, documentos) ->
                if (fecha in fechas) {
                    var totalCalorias = 0
                    var totalMinutos = 0
                    var totalPasos = 0
                    var totalDistancia = 0.0
                    var totalRecorridos = 0

                    documentos.forEach { documento ->
                        try {
                            val data = documento.data
                            if (data != null) {
                                totalCalorias += (data["caloriasQuemadas"] as? Number)?.toInt() ?: 0
                                totalMinutos += (data["duracionMinutos"] as? Number)?.toInt() ?: 0
                                totalPasos += (data["pasos"] as? Number)?.toInt() ?: 0
                                totalDistancia += (data["distanciaKm"] as? Number)?.toDouble() ?: 0.0
                                totalRecorridos++
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error procesando documento ${documento.id}: ${e.message}", e)
                        }
                    }

                    estadisticasPorFecha[fecha] = RecorridoRepository.EstadisticasDiarias(
                        totalCalorias = totalCalorias,
                        totalMinutos = totalMinutos,
                        totalPasos = totalPasos,
                        totalDistanciaKm = totalDistancia,
                        totalRecorridos = totalRecorridos
                    )

                    Log.d(TAG, "📈 $fecha: ${totalPasos} pasos, ${totalCalorias} cal, ${String.format("%.2f", totalDistancia)} km")
                }
            }

            Log.d(TAG, "✅ Estadísticas calculadas para ${estadisticasPorFecha.size} fechas")
            Result.success(estadisticasPorFecha)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo estadísticas para gráficos: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener estadísticas de la semana actual
     */
    suspend fun obtenerEstadisticasSemanal(userId: String): Result<RecorridoRepository.EstadisticasDiarias> {
        return try {
            val fechas = obtenerFechasSemanaActual()
            val estadisticasPorFecha = obtenerEstadisticasParaGraficos(userId, fechas)

            if (estadisticasPorFecha.isSuccess) {
                val todasLasEstadisticas = estadisticasPorFecha.getOrNull() ?: emptyMap()

                val resumenSemanal = todasLasEstadisticas.values.fold(
                    RecorridoRepository.EstadisticasDiarias()
                ) { acumulado, estadistica ->
                    RecorridoRepository.EstadisticasDiarias(
                        totalCalorias = acumulado.totalCalorias + estadistica.totalCalorias,
                        totalMinutos = acumulado.totalMinutos + estadistica.totalMinutos,
                        totalPasos = acumulado.totalPasos + estadistica.totalPasos,
                        totalDistanciaKm = acumulado.totalDistanciaKm + estadistica.totalDistanciaKm,
                        totalRecorridos = acumulado.totalRecorridos + estadistica.totalRecorridos
                    )
                }

                Log.d(TAG, "📊 Resumen semanal: ${resumenSemanal.totalPasos} pasos, ${resumenSemanal.totalCalorias} cal")
                Result.success(resumenSemanal)
            } else {
                Result.failure(estadisticasPorFecha.exceptionOrNull() ?: Exception("Error desconocido"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo estadísticas semanales: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener fechas de la semana actual (Lunes a Domingo)
     */
    private fun obtenerFechasSemanaActual(): List<String> {
        val fechas = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Ir al lunes de esta semana
        val diaDeLaSemana = calendar.get(Calendar.DAY_OF_WEEK)
        val diasHastaLunes = when (diaDeLaSemana) {
            Calendar.SUNDAY -> -6
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> -1
            Calendar.WEDNESDAY -> -2
            Calendar.THURSDAY -> -3
            Calendar.FRIDAY -> -4
            Calendar.SATURDAY -> -5
            else -> 0
        }

        calendar.add(Calendar.DAY_OF_YEAR, diasHastaLunes)

        // Agregar 7 días (Lunes a Domingo)
        for (i in 0..6) {
            fechas.add(formato.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return fechas
    }

    /**
     * Obtener estadísticas de un mes específico
     */
    suspend fun obtenerEstadisticasMensual(
        userId: String,
        año: Int,
        mes: Int
    ): Result<Map<Int, RecorridoRepository.EstadisticasDiarias>> {
        return try {
            Log.d(TAG, "📅 Obteniendo estadísticas mensuales: $mes/$año")

            val calendar = Calendar.getInstance()
            calendar.set(año, mes - 1, 1) // mes - 1 porque Calendar usa 0-based
            val primerDia = calendar.get(Calendar.DAY_OF_MONTH)
            val ultimoDia = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

            val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val fechas = mutableListOf<String>()

            // Generar todas las fechas del mes
            for (dia in primerDia..ultimoDia) {
                calendar.set(año, mes - 1, dia)
                fechas.add(formato.format(calendar.time))
            }

            val estadisticasPorFecha = obtenerEstadisticasParaGraficos(userId, fechas)

            if (estadisticasPorFecha.isSuccess) {
                val datos = estadisticasPorFecha.getOrNull() ?: emptyMap()
                val estadisticasPorDia = mutableMapOf<Int, RecorridoRepository.EstadisticasDiarias>()

                fechas.forEachIndexed { index, fecha ->
                    val dia = index + 1
                    estadisticasPorDia[dia] = datos[fecha] ?: RecorridoRepository.EstadisticasDiarias()
                }

                Log.d(TAG, "✅ Estadísticas mensuales calculadas para ${estadisticasPorDia.size} días")
                Result.success(estadisticasPorDia)
            } else {
                Result.failure(estadisticasPorFecha.exceptionOrNull() ?: Exception("Error desconocido"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo estadísticas mensuales: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener tendencias de actividad (comparar semana actual vs anterior)
     */
    suspend fun obtenerTendencias(userId: String): Result<TendenciasActividad> {
        return try {
            Log.d(TAG, "📈 Calculando tendencias de actividad")

            // Obtener fechas de esta semana y la anterior
            val fechasSemanaActual = obtenerFechasSemanaActual()
            val fechasSemanaAnterior = obtenerFechasSemanaAnterior()

            // Obtener estadísticas para ambas semanas
            val estadisticasActual = obtenerEstadisticasSemanal(userId)
            val estadisticasAnterior = obtenerEstadisticasParaGraficos(userId, fechasSemanaAnterior)

            if (estadisticasActual.isSuccess && estadisticasAnterior.isSuccess) {
                val actual = estadisticasActual.getOrNull()!!
                val anterior = estadisticasAnterior.getOrNull()!!.values.fold(
                    RecorridoRepository.EstadisticasDiarias()
                ) { acc, stats ->
                    RecorridoRepository.EstadisticasDiarias(
                        totalCalorias = acc.totalCalorias + stats.totalCalorias,
                        totalMinutos = acc.totalMinutos + stats.totalMinutos,
                        totalPasos = acc.totalPasos + stats.totalPasos,
                        totalDistanciaKm = acc.totalDistanciaKm + stats.totalDistanciaKm,
                        totalRecorridos = acc.totalRecorridos + stats.totalRecorridos
                    )
                }

                val tendencias = TendenciasActividad(
                    cambiosPasos = calcularCambioPercentual(anterior.totalPasos, actual.totalPasos),
                    cambiosCalorias = calcularCambioPercentual(anterior.totalCalorias, actual.totalCalorias),
                    cambiosDistancia = calcularCambioPercentual(anterior.totalDistanciaKm, actual.totalDistanciaKm),
                    cambiosMinutos = calcularCambioPercentual(anterior.totalMinutos, actual.totalMinutos),
                    semanaActual = actual,
                    semanaAnterior = anterior
                )

                Log.d(TAG, "✅ Tendencias calculadas: Pasos ${tendencias.cambiosPasos}%")
                Result.success(tendencias)
            } else {
                Result.failure(Exception("Error obteniendo datos para tendencias"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error calculando tendencias: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener fechas de la semana anterior
     */
    private fun obtenerFechasSemanaAnterior(): List<String> {
        val fechas = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Ir al lunes de la semana pasada
        val diaDeLaSemana = calendar.get(Calendar.DAY_OF_WEEK)
        val diasHastaLunesAnterior = when (diaDeLaSemana) {
            Calendar.SUNDAY -> -13
            Calendar.MONDAY -> -7
            Calendar.TUESDAY -> -8
            Calendar.WEDNESDAY -> -9
            Calendar.THURSDAY -> -10
            Calendar.FRIDAY -> -11
            Calendar.SATURDAY -> -12
            else -> -7
        }

        calendar.add(Calendar.DAY_OF_YEAR, diasHastaLunesAnterior)

        // Agregar 7 días
        for (i in 0..6) {
            fechas.add(formato.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return fechas
    }

    /**
     * Calcular cambio porcentual entre dos valores
     */
    private fun calcularCambioPercentual(valorAnterior: Int, valorActual: Int): Int {
        return if (valorAnterior == 0) {
            if (valorActual > 0) 100 else 0
        } else {
            (((valorActual - valorAnterior).toFloat() / valorAnterior) * 100).toInt()
        }
    }

    private fun calcularCambioPercentual(valorAnterior: Double, valorActual: Double): Int {
        return if (valorAnterior == 0.0) {
            if (valorActual > 0) 100 else 0
        } else {
            (((valorActual - valorAnterior) / valorAnterior) * 100).toInt()
        }
    }

    /**
     * Data class para tendencias de actividad
     */
    data class TendenciasActividad(
        val cambiosPasos: Int,
        val cambiosCalorias: Int,
        val cambiosDistancia: Int,
        val cambiosMinutos: Int,
        val semanaActual: RecorridoRepository.EstadisticasDiarias,
        val semanaAnterior: RecorridoRepository.EstadisticasDiarias
    )

    /**
     * Obtener el resumen de actividad más reciente
     */
    suspend fun obtenerResumenReciente(userId: String, diasAtras: Int = 30): Result<ResumenActividad> {
        return try {
            Log.d(TAG, "📋 Obteniendo resumen de últimos $diasAtras días")

            val fechas = mutableListOf<String>()
            val calendar = Calendar.getInstance()
            val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // Generar fechas de los últimos N días
            for (i in diasAtras - 1 downTo 0) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                fechas.add(formato.format(calendar.time))
            }

            val estadisticasPorFecha = obtenerEstadisticasParaGraficos(userId, fechas)

            if (estadisticasPorFecha.isSuccess) {
                val datos = estadisticasPorFecha.getOrNull() ?: emptyMap()

                // Calcular totales
                val totales = datos.values.fold(RecorridoRepository.EstadisticasDiarias()) { acc, stats ->
                    RecorridoRepository.EstadisticasDiarias(
                        totalCalorias = acc.totalCalorias + stats.totalCalorias,
                        totalMinutos = acc.totalMinutos + stats.totalMinutos,
                        totalPasos = acc.totalPasos + stats.totalPasos,
                        totalDistanciaKm = acc.totalDistanciaKm + stats.totalDistanciaKm,
                        totalRecorridos = acc.totalRecorridos + stats.totalRecorridos
                    )
                }

                // Calcular promedios
                val diasConActividad = datos.values.count { it.totalRecorridos > 0 }
                val promedios = if (diasConActividad > 0) {
                    RecorridoRepository.EstadisticasDiarias(
                        totalCalorias = totales.totalCalorias / diasConActividad,
                        totalMinutos = totales.totalMinutos / diasConActividad,
                        totalPasos = totales.totalPasos / diasConActividad,
                        totalDistanciaKm = totales.totalDistanciaKm / diasConActividad,
                        totalRecorridos = totales.totalRecorridos / diasConActividad
                    )
                } else {
                    RecorridoRepository.EstadisticasDiarias()
                }

                // Encontrar día más activo
                val diaMasActivo = datos.maxByOrNull { it.value.totalPasos }

                val resumen = ResumenActividad(
                    totalDias = diasAtras,
                    diasConActividad = diasConActividad,
                    totales = totales,
                    promediosDiarios = promedios,
                    diaMasActivoFecha = diaMasActivo?.key ?: "",
                    diaMasActivoStats = diaMasActivo?.value ?: RecorridoRepository.EstadisticasDiarias()
                )

                Log.d(TAG, "✅ Resumen calculado: $diasConActividad/$diasAtras días activos")
                Result.success(resumen)
            } else {
                Result.failure(estadisticasPorFecha.exceptionOrNull() ?: Exception("Error desconocido"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo resumen reciente: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Data class para resumen de actividad
     */
    data class ResumenActividad(
        val totalDias: Int,
        val diasConActividad: Int,
        val totales: RecorridoRepository.EstadisticasDiarias,
        val promediosDiarios: RecorridoRepository.EstadisticasDiarias,
        val diaMasActivoFecha: String,
        val diaMasActivoStats: RecorridoRepository.EstadisticasDiarias
    )
}