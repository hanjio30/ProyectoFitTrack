package com.example.fittrack.network

import android.util.Log
import com.example.fittrack.model.EstadisticasUsuario
import com.example.fittrack.model.MetaDiaria
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class MetaDiariaRepository {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "MetaDiariaRepository"

    suspend fun getMetaDiariaActual(userId: String): Result<MetaDiaria> {
        return try {
            val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val documento = db.collection("users")
                .document(userId)
                .collection("metas")
                .document(fechaHoy)
                .get()
                .await()

            if (documento.exists()) {
                val meta = documento.toObject(MetaDiaria::class.java)
                if (meta != null) {
                    // ✅ USAR LOS VALORES DE LA BASE DE DATOS DIRECTAMENTE
                    // Si ya están calculados en la BD, úsalos
                    val progresoActual = meta.progresoActual
                    val porcentajeCalculado = meta.porcentajeCompletado
                    val puntosCalculados = meta.puntosGanados

                    Log.d(TAG, "Meta cargada - Progreso: $progresoActual, Porcentaje: $porcentajeCalculado, Puntos: $puntosCalculados")

                    val metaActualizada = meta.copy(
                        progresoActual = progresoActual,
                        porcentajeCompletado = porcentajeCalculado,
                        puntosGanados = puntosCalculados,
                        metaAlcanzada = porcentajeCalculado >= 100
                    )
                    Result.success(metaActualizada)
                } else {
                    Result.failure(Exception("Error al deserializar meta"))
                }
            } else {
                // Crear meta por defecto si no existe
                val progresoInicial = calcularProgresoDelDia(userId, fechaHoy)
                val metaPorDefecto = MetaDiaria(
                    id = fechaHoy,
                    userId = userId,
                    fecha = fechaHoy,
                    metaKilometros = 10.0,
                    progresoActual = progresoInicial,
                    porcentajeCompletado = calcularPorcentaje(progresoInicial, 10.0),
                    puntosGanados = calcularPuntos(progresoInicial, 10.0),
                    metaAlcanzada = progresoInicial >= 10.0
                )

                // Guardar la meta por defecto
                crearMetaDiaria(metaPorDefecto)
                Result.success(metaPorDefecto)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener meta diaria: ${e.message}", e)
            Result.failure(e)
        }
    }


    // Calcular progreso del día basado en recorridos
    private suspend fun calcularProgresoDelDia(userId: String, fecha: String): Double {
        return try {
            val recorridos = db.collection("users")
                .document(userId)
                .collection("recorridos")
                .whereEqualTo("fecha", fecha)
                .get()
                .await()

            var totalDistancia = 0.0
            for (documento in recorridos.documents) {
                val distancia = documento.getDouble("distanciaKm") ?: 0.0
                totalDistancia += distancia
            }

            Log.d(TAG, "Progreso calculado para $fecha: $totalDistancia km")
            totalDistancia
        } catch (e: Exception) {
            Log.e(TAG, "Error al calcular progreso: ${e.message}", e)
            0.0
        }
    }

    // Crear nueva meta diaria
    suspend fun crearMetaDiaria(meta: MetaDiaria): Result<Boolean> {
        return try {
            db.collection("users")
                .document(meta.userId)
                .collection("metas")
                .document(meta.fecha)
                .set(meta)
                .await()

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear meta: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Actualizar meta diaria
    suspend fun actualizarMeta(userId: String, fecha: String, nuevaMetaKm: Double): Result<MetaDiaria> {
        return try {
            val progresoActual = calcularProgresoDelDia(userId, fecha)

            val datosActualizados = mapOf(
                "metaKilometros" to nuevaMetaKm,
                "progresoActual" to progresoActual,
                "porcentajeCompletado" to calcularPorcentaje(progresoActual, nuevaMetaKm),
                "puntosGanados" to calcularPuntos(progresoActual, nuevaMetaKm),
                "metaAlcanzada" to (progresoActual >= nuevaMetaKm),
                "updatedAt" to System.currentTimeMillis()
            )

            db.collection("users")
                .document(userId)
                .collection("metas")
                .document(fecha)
                .update(datosActualizados)
                .await()

            // Obtener la meta actualizada
            getMetaDiariaActual(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar meta: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Obtener estadísticas del usuario
    suspend fun getEstadisticasUsuario(userId: String): Result<EstadisticasUsuario> {
        return try {
            // Obtener últimas 30 metas
            val metas = db.collection("users")
                .document(userId)
                .collection("metas")
                .orderBy("fecha", Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .await()

            var diasConsecutivos = 0
            var metasAlcanzadas = 0
            var mejorRacha = 0
            var rachaActual = 0
            var totalKilometros = 0.0

            val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            var esConsecutivo = true

            for (documento in metas.documents) {
                val meta = documento.toObject(MetaDiaria::class.java)
                meta?.let {
                    totalKilometros += it.progresoActual

                    if (it.metaAlcanzada) {
                        metasAlcanzadas++
                        if (esConsecutivo) {
                            rachaActual++
                            diasConsecutivos = rachaActual
                        } else {
                            rachaActual = 1
                        }

                        if (rachaActual > mejorRacha) {
                            mejorRacha = rachaActual
                        }
                    } else {
                        esConsecutivo = false
                        rachaActual = 0
                    }
                }
            }

            val promedioSemanal = if (metas.size() > 0) totalKilometros / minOf(metas.size(), 7) else 0.0

            val estadisticas = EstadisticasUsuario(
                diasConsecutivos = diasConsecutivos,
                metasAlcanzadas = metasAlcanzadas,
                mejorRacha = mejorRacha,
                promedioSemanal = promedioSemanal,
                totalKilometros = totalKilometros
            )

            Result.success(estadisticas)
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener estadísticas: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun calcularPorcentaje(progreso: Double, meta: Double): Int {
        return if (meta > 0) {
            ((progreso / meta) * 100).toInt()  // ✅ QUITAR .coerceAtMost(100)
        } else 0
    }

    private fun calcularPuntos(progreso: Double, meta: Double): Int {
        val porcentaje = if (meta > 0) ((progreso / meta) * 100).toInt() else 0
        return when {
            porcentaje >= 100 -> 50 + ((porcentaje - 100) / 10) * 5  // Puntos extra por superar meta
            porcentaje >= 75 -> 35
            porcentaje >= 50 -> 25
            porcentaje >= 25 -> 15
            else -> 5
        }
    }
}