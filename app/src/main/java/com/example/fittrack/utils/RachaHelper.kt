package com.example.fittrack.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper class para gestionar la integración entre metas diarias y racha
 */
object RachaHelper {

    private const val TAG = "RachaHelper"
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_METAS = "metas"
    private const val COLLECTION_RACHA_DATA = "rachaData"

    private val db = FirebaseFirestore.getInstance()

    /**
     * Actualiza la racha cuando se completa una meta diaria
     */
    suspend fun updateRachaOnMetaCompletion(userId: String, metaCompletada: Boolean = true) {
        try {
            Log.d(TAG, "Actualizando racha para usuario: $userId")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = dateFormat.format(Date())

            // Verificar si hoy hay metas completadas al 100%
            val hasCompletedMetaToday = checkMetaCompletedForDate(userId, today)

            if (hasCompletedMetaToday) {
                // Calcular nueva racha
                val newStreak = calculateCurrentStreak(userId)

                // Obtener datos actuales de racha
                val currentRachaData = getCurrentRachaData(userId)

                // Actualizar mejor racha si es necesario
                val newBestStreak = maxOf(newStreak, currentRachaData.bestStreak)

                // Calcular puntos totales
                val totalPoints = calculateTotalPoints(userId, newStreak)

                // Guardar datos actualizados
                saveRachaData(userId, newStreak, newBestStreak, totalPoints, today)

                Log.d(TAG, "Racha actualizada: $newStreak días, mejor: $newBestStreak, puntos: $totalPoints")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar racha: ${e.message}", e)
        }
    }

    /**
     * Verifica si hay metas completadas para una fecha específica
     */
    private suspend fun checkMetaCompletedForDate(userId: String, date: String): Boolean {
        return try {
            val metasQuery = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_METAS)
                .whereEqualTo("fecha", date)
                .get()
                .await()

            // Verificar si hay al menos una meta completada al 100%
            metasQuery.documents.any { doc ->
                val porcentaje = doc.getLong("porcentajeCompletado")?.toInt() ?: 0
                porcentaje >= 100
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar meta para fecha $date: ${e.message}", e)
            false
        }
    }

    /**
     * Calcula la racha actual basada en metas completadas
     */
    private suspend fun calculateCurrentStreak(userId: String): Int {
        return try {
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            var consecutiveDays = 0
            val today = dateFormat.format(Date())

            // Verificar si hoy ya se completó una meta
            val todayCompleted = checkMetaCompletedForDate(userId, today)

            // Si hoy está completado, empezar contando desde hoy
            if (todayCompleted) {
                consecutiveDays = 1
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }

            // Verificar días consecutivos hacia atrás
            for (i in 0 until 364) {
                val currentDate = dateFormat.format(calendar.time)
                val hasCompletedMeta = checkMetaCompletedForDate(userId, currentDate)

                if (hasCompletedMeta) {
                    consecutiveDays++
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }

            consecutiveDays

        } catch (e: Exception) {
            Log.e(TAG, "Error al calcular racha: ${e.message}", e)
            0
        }
    }

    /**
     * Obtiene los datos actuales de racha
     */
    private suspend fun getCurrentRachaData(userId: String): RachaData {
        return try {
            val rachaDoc = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_RACHA_DATA)
                .document("current")
                .get()
                .await()

            if (rachaDoc.exists()) {
                RachaData(
                    currentStreak = rachaDoc.getLong("currentStreak")?.toInt() ?: 0,
                    bestStreak = rachaDoc.getLong("bestStreak")?.toInt() ?: 0,
                    totalPoints = rachaDoc.getLong("totalPoints")?.toInt() ?: 0,
                    lastUpdateDate = rachaDoc.getString("lastUpdateDate") ?: ""
                )
            } else {
                RachaData()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener datos de racha: ${e.message}", e)
            RachaData()
        }
    }

    /**
     * Calcula los puntos totales incluyendo puntos de metas y bonus de racha
     */
    private suspend fun calculateTotalPoints(userId: String, currentStreak: Int): Int {
        return try {
            // Puntos de metas completadas
            val metasQuery = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_METAS)
                .whereGreaterThanOrEqualTo("porcentajeCompletado", 100)
                .get()
                .await()

            var pointsFromMetas = 0
            metasQuery.documents.forEach { doc ->
                val puntosGanados = doc.getLong("puntosGanados")?.toInt() ?: 0
                pointsFromMetas += puntosGanados
            }

            // Puntos de bonificación por racha
            val bonusPoints = calculateStreakBonusPoints(currentStreak)

            pointsFromMetas + bonusPoints

        } catch (e: Exception) {
            Log.e(TAG, "Error al calcular puntos totales: ${e.message}", e)
            0
        }
    }

    /**
     * Calcula los puntos de bonificación por racha
     */
    private fun calculateStreakBonusPoints(streak: Int): Int {
        return when {
            streak >= 30 -> streak * 10 // 10 puntos por día para maestros
            streak >= 20 -> streak * 8  // 8 puntos por día para expertos
            streak >= 10 -> streak * 6  // 6 puntos por día para avanzados
            streak >= 5 -> streak * 4   // 4 puntos por día para intermedios
            else -> streak * 2          // 2 puntos por día para principiantes
        }
    }

    /**
     * Guarda los datos de racha en Firebase
     */
    private suspend fun saveRachaData(
        userId: String,
        currentStreak: Int,
        bestStreak: Int,
        totalPoints: Int,
        lastUpdateDate: String
    ) {
        try {
            val rachaDocument = hashMapOf(
                "currentStreak" to currentStreak,
                "bestStreak" to bestStreak,
                "totalPoints" to totalPoints,
                "lastUpdateDate" to lastUpdateDate,
                "updatedAt" to System.currentTimeMillis()
            )

            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_RACHA_DATA)
                .document("current")
                .set(rachaDocument)
                .await()

            // También guardar en historial
            saveRachaHistory(userId, currentStreak, lastUpdateDate, totalPoints)

        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar datos de racha: ${e.message}", e)
        }
    }

    /**
     * Guarda el historial diario de racha
     */
    private suspend fun saveRachaHistory(
        userId: String,
        streak: Int,
        date: String,
        totalPoints: Int
    ) {
        try {
            val historyDocument = hashMapOf(
                "streak" to streak,
                "date" to date,
                "totalPoints" to totalPoints,
                "bonusPoints" to calculateStreakBonusPoints(streak),
                "level" to getStreakLevel(streak),
                "timestamp" to System.currentTimeMillis()
            )

            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_RACHA_DATA)
                .document("history")
                .collection("daily")
                .document(date)
                .set(historyDocument)
                .await()

        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar historial de racha: ${e.message}", e)
        }
    }

    /**
     * Obtiene el nivel de racha basado en días consecutivos
     */
    private fun getStreakLevel(streak: Int): String {
        return when {
            streak >= 30 -> "Maestro"
            streak >= 20 -> "Experto"
            streak >= 10 -> "Avanzado"
            streak >= 5 -> "Intermedio"
            else -> "Principiante"
        }
    }

    /**
     * Función para llamar desde otros fragmentos cuando se complete una meta
     */
    suspend fun onMetaCompleted(userId: String) {
        updateRachaOnMetaCompletion(userId, true)
    }

    /**
     * Función para resetear la racha si no se completan metas
     */
    suspend fun checkAndResetStreak(userId: String) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val yesterday = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }
            val yesterdayDate = dateFormat.format(yesterday.time)

            val yesterdayCompleted = checkMetaCompletedForDate(userId, yesterdayDate)

            if (!yesterdayCompleted) {
                // Resetear racha pero mantener mejor racha y puntos acumulados
                val currentData = getCurrentRachaData(userId)
                saveRachaData(userId, 0, currentData.bestStreak, currentData.totalPoints, yesterdayDate)

                Log.d(TAG, "Racha reseteada por falta de actividad en $yesterdayDate")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar/resetear racha: ${e.message}", e)
        }
    }

    /**
     * Data class para manejar datos de racha
     */
    data class RachaData(
        val currentStreak: Int = 0,
        val bestStreak: Int = 0,
        val totalPoints: Int = 0,
        val lastUpdateDate: String = ""
    )
}