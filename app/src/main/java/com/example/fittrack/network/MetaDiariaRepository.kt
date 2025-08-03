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
                    // ✅ SIEMPRE RECALCULAR EL PROGRESO ACTUAL DESDE LOS RECORRIDOS
                    val progresoRecalculado = calcularProgresoDelDia(userId, fechaHoy)

                    Log.d(TAG, "Meta cargada - Progreso BD: ${meta.progresoActual}, Progreso recalculado: $progresoRecalculado")

                    val metaActualizada = meta.copy(
                        progresoActual = progresoRecalculado,
                        porcentajeCompletado = calcularPorcentaje(progresoRecalculado, meta.metaKilometros),
                        puntosGanados = calcularPuntos(progresoRecalculado, meta.metaKilometros),
                        metaAlcanzada = progresoRecalculado >= meta.metaKilometros
                    )

                    // Actualizar la meta en la BD con los nuevos valores
                    actualizarProgresoEnBD(userId, fechaHoy, metaActualizada)

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

    // ✨ NUEVA FUNCIÓN: Actualizar automáticamente cuando se agrega un recorrido
    suspend fun actualizarMetaTrasNuevoRecorrido(userId: String, fechaRecorrido: String): Result<MetaDiaria> {
        return try {
            Log.d(TAG, "Actualizando meta tras nuevo recorrido para fecha: $fechaRecorrido")

            // Calcular el nuevo progreso basado en todos los recorridos del día
            val nuevoProgreso = calcularProgresoDelDia(userId, fechaRecorrido)

            // Verificar si existe la meta para ese día
            val documentoMeta = db.collection("users")
                .document(userId)
                .collection("metas")
                .document(fechaRecorrido)
                .get()
                .await()

            val metaActualizada = if (documentoMeta.exists()) {
                // Meta existe, actualizarla
                val metaExistente = documentoMeta.toObject(MetaDiaria::class.java)!!

                metaExistente.copy(
                    progresoActual = nuevoProgreso,
                    porcentajeCompletado = calcularPorcentaje(nuevoProgreso, metaExistente.metaKilometros),
                    puntosGanados = calcularPuntos(nuevoProgreso, metaExistente.metaKilometros),
                    metaAlcanzada = nuevoProgreso >= metaExistente.metaKilometros
                )
            } else {
                // Meta no existe, crear una nueva
                MetaDiaria(
                    id = fechaRecorrido,
                    userId = userId,
                    fecha = fechaRecorrido,
                    metaKilometros = 10.0, // Meta por defecto
                    progresoActual = nuevoProgreso,
                    porcentajeCompletado = calcularPorcentaje(nuevoProgreso, 10.0),
                    puntosGanados = calcularPuntos(nuevoProgreso, 10.0),
                    metaAlcanzada = nuevoProgreso >= 10.0
                )
            }

            // Guardar/actualizar en la BD
            actualizarProgresoEnBD(userId, fechaRecorrido, metaActualizada)

            Log.d(TAG, "Meta actualizada - Nuevo progreso: $nuevoProgreso km")
            Result.success(metaActualizada)

        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar meta tras recorrido: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ✨ FUNCIÓN AUXILIAR: Actualizar solo el progreso en la BD
    private suspend fun actualizarProgresoEnBD(userId: String, fecha: String, meta: MetaDiaria) {
        try {
            val datosActualizados = mapOf(
                "progresoActual" to meta.progresoActual,
                "porcentajeCompletado" to meta.porcentajeCompletado,
                "puntosGanados" to meta.puntosGanados,
                "metaAlcanzada" to meta.metaAlcanzada,
                "updatedAt" to System.currentTimeMillis()
            )

            db.collection("users")
                .document(userId)
                .collection("metas")
                .document(fecha)
                .set(meta) // Usar set en lugar de update para crear si no existe
                .await()

            Log.d(TAG, "Progreso actualizado en BD - Fecha: $fecha, Progreso: ${meta.progresoActual}")
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar progreso en BD: ${e.message}", e)
        }
    }

    // ✨ MEJORAR: Calcular progreso del día basado en recorridos (con más logs)
    private suspend fun calcularProgresoDelDia(userId: String, fecha: String): Double {
        return try {
            Log.d(TAG, "Calculando progreso para usuario: $userId, fecha: $fecha")

            val recorridos = db.collection("users")
                .document(userId)
                .collection("recorridos")
                .whereEqualTo("fecha", fecha)
                .get()
                .await()

            var totalDistancia = 0.0
            var contadorRecorridos = 0

            for (documento in recorridos.documents) {
                val distancia = documento.getDouble("distanciaKm") ?: 0.0
                totalDistancia += distancia
                contadorRecorridos++

                Log.d(TAG, "Recorrido encontrado - ID: ${documento.id}, Distancia: $distancia km")
            }

            Log.d(TAG, "Progreso calculado para $fecha: $totalDistancia km ($contadorRecorridos recorridos)")
            totalDistancia

        } catch (e: Exception) {
            Log.e(TAG, "Error al calcular progreso: ${e.message}", e)
            0.0
        }
    }

    // ✨ NUEVA FUNCIÓN: Obtener progreso en tiempo real
    suspend fun getProgresoTiempoReal(userId: String): Result<Double> {
        return try {
            val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val progreso = calcularProgresoDelDia(userId, fechaHoy)
            Result.success(progreso)
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener progreso en tiempo real: ${e.message}", e)
            Result.failure(e)
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
            ((progreso / meta) * 100).toInt()
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