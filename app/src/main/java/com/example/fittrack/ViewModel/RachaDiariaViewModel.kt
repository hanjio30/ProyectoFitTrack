package com.example.fittrack.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class RachaDiariaViewModel : ViewModel() {

    companion object {
        private const val TAG = "RachaDiariaViewModel"
        private const val COLLECTION_METAS = "metas"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_RACHA_DATA = "rachaData" // Nueva colección para datos de racha
    }

    private val db = FirebaseFirestore.getInstance()

    // LiveData para los datos de racha
    private val _currentStreak = MutableLiveData<Int>()
    val currentStreak: LiveData<Int> = _currentStreak

    private val _streakLevel = MutableLiveData<String>()
    val streakLevel: LiveData<String> = _streakLevel

    private val _bestStreak = MutableLiveData<Int>()
    val bestStreak: LiveData<Int> = _bestStreak

    private val _totalPoints = MutableLiveData<Int>()
    val totalPoints: LiveData<Int> = _totalPoints

    private val _nextMilestone = MutableLiveData<MilestoneInfo>()
    val nextMilestone: LiveData<MilestoneInfo> = _nextMilestone

    private val _motivationalMessage = MutableLiveData<String>()
    val motivationalMessage: LiveData<String> = _motivationalMessage

    private val _dailyTip = MutableLiveData<String>()
    val dailyTip: LiveData<String> = _dailyTip

    private val _weeklyHistory = MutableLiveData<List<DayStatus>>()
    val weeklyHistory: LiveData<List<DayStatus>> = _weeklyHistory

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Variables internas para cálculos
    private var userId: String? = null
    private var streakData: StreakData? = null

    fun loadStreakData(userId: String?) {
        if (userId.isNullOrEmpty()) {
            Log.e(TAG, "UserId es nulo o vacío")
            _errorMessage.value = "Error: Usuario no identificado"
            return
        }

        this.userId = userId
        viewModelScope.launch {
            try {
                Log.d(TAG, "Cargando datos de racha para: $userId")
                _isLoading.value = true

                // Cargar datos de racha guardados
                loadSavedStreakData()

                // Calcular racha actual basada en metas
                calculateCurrentStreak()

                // Cargar historial semanal (solo semana actual)
                loadCurrentWeekHistory()

                // Calcular puntos totales (racha + metas diarias)
                calculateTotalPoints()

                // Actualizar UI inmediatamente
                updateUI()

                // Guardar datos actualizados de racha
                saveStreakData()

                _isLoading.value = false
                Log.d(TAG, "Datos de racha cargados y mostrados exitosamente")

            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar datos: ${e.message}", e)
                _errorMessage.value = "Error al cargar estadísticas de racha"
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadSavedStreakData() {
        try {
            // Cargar datos específicos de racha
            val rachaDoc = db.collection(COLLECTION_USERS)
                .document(userId!!)
                .collection(COLLECTION_RACHA_DATA)
                .document("current")
                .get()
                .await()

            if (rachaDoc.exists()) {
                val bestStreak = rachaDoc.getLong("bestStreak")?.toInt() ?: 0
                val currentStreak = rachaDoc.getLong("currentStreak")?.toInt() ?: 0
                val streakPoints = rachaDoc.getLong("streakPoints")?.toInt() ?: 0
                val lastUpdate = rachaDoc.getString("lastUpdate") ?: ""

                streakData = StreakData(
                    currentStreak = currentStreak,
                    bestStreak = bestStreak,
                    streakPoints = streakPoints,
                    lastUpdate = lastUpdate
                )

                Log.d(TAG, "Datos de racha cargados: currentStreak=$currentStreak, bestStreak=$bestStreak, streakPoints=$streakPoints")
            } else {
                // Crear datos iniciales de racha
                streakData = StreakData()
                createInitialStreakData()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar datos de racha: ${e.message}", e)
            streakData = StreakData() // Usar datos por defecto
        }
    }

    private suspend fun calculateCurrentStreak() {
        try {
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            var consecutiveDays = 0

            // Verificar días consecutivos hacia atrás desde hoy
            for (i in 0 until 365) { // Máximo 365 días hacia atrás
                val currentDate = dateFormat.format(calendar.time)

                val hasCompletedMeta = checkMetaCompletedForDate(currentDate)

                if (hasCompletedMeta) {
                    consecutiveDays++
                    calendar.add(Calendar.DAY_OF_YEAR, -1) // Ir un día atrás
                } else {
                    // Si es el día actual (i == 0), no romper la racha aún
                    if (i == 0) {
                        calendar.add(Calendar.DAY_OF_YEAR, -1)
                        continue
                    }
                    break // Romper la racha si no completó un día anterior
                }
            }

            streakData?.currentStreak = consecutiveDays
            Log.d(TAG, "Racha actual calculada: $consecutiveDays días")

        } catch (e: Exception) {
            Log.e(TAG, "Error al calcular racha: ${e.message}", e)
            streakData?.currentStreak = 0
        }
    }

    private suspend fun checkMetaCompletedForDate(date: String): Boolean {
        return try {
            val metas = db.collection(COLLECTION_USERS)
                .document(userId!!)
                .collection(COLLECTION_METAS)
                .whereEqualTo("fecha", date)
                .get()
                .await()

            // Verificar si hay al menos una meta completada al 100%
            metas.documents.any { doc ->
                val porcentaje = doc.getLong("porcentajeCompletado")?.toInt() ?: 0
                porcentaje >= 100
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar meta para fecha $date: ${e.message}", e)
            false
        }
    }

    private suspend fun loadCurrentWeekHistory() {
        try {
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val weekHistory = mutableListOf<DayStatus>()

            // Obtener el inicio de la semana actual (Lunes)
            calendar.firstDayOfWeek = Calendar.MONDAY
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

            // Crear historial para los 7 días de la semana actual
            for (i in 0 until 7) {
                val date = dateFormat.format(calendar.time)
                val completed = checkMetaCompletedForDate(date)
                val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time)

                weekHistory.add(DayStatus(
                    date = date,
                    dayName = dayName,
                    completed = completed
                ))

                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            _weeklyHistory.value = weekHistory
            Log.d(TAG, "Historial semanal actual cargado: ${weekHistory.size} días")

        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar historial semanal: ${e.message}", e)
        }
    }

    private suspend fun calculateTotalPoints() {
        try {
            // Obtener puntos de racha
            val streakPoints = streakData?.streakPoints ?: 0

            // Obtener puntos de metas diarias completadas
            val metasPoints = getTotalMetasPoints()

            // Sumar ambos tipos de puntos
            val total = streakPoints + metasPoints
            streakData?.totalPoints = total

            Log.d(TAG, "Puntos totales calculados: streakPoints=$streakPoints + metasPoints=$metasPoints = $total")

        } catch (e: Exception) {
            Log.e(TAG, "Error al calcular puntos totales: ${e.message}", e)
            streakData?.totalPoints = 0
        }
    }

    private suspend fun getTotalMetasPoints(): Int {
        return try {
            val metas = db.collection(COLLECTION_USERS)
                .document(userId!!)
                .collection(COLLECTION_METAS)
                .whereEqualTo("metaAlcanzada", true)
                .get()
                .await()

            var totalMetasPoints = 0
            metas.documents.forEach { doc ->
                val puntosGanados = doc.getLong("puntosGanados")?.toInt() ?: 0
                totalMetasPoints += puntosGanados
            }

            Log.d(TAG, "Puntos totales de metas: $totalMetasPoints")
            totalMetasPoints

        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener puntos de metas: ${e.message}", e)
            0
        }
    }

    private suspend fun updateUI() {
        streakData?.let { data ->
            // Actualizar inmediatamente los LiveData
            _currentStreak.value = data.currentStreak
            _bestStreak.value = data.bestStreak
            _totalPoints.value = data.totalPoints

            // Actualizar nivel basado en racha actual
            _streakLevel.value = getStreakLevel(data.currentStreak)

            // Calcular próximo hito
            _nextMilestone.value = calculateNextMilestone(data.currentStreak)

            // Mensajes motivacionales
            _motivationalMessage.value = getMotivationalMessage(data.currentStreak)
            _dailyTip.value = getDailyTip()

            // Actualizar mejor racha si es necesario
            if (data.currentStreak > data.bestStreak) {
                data.bestStreak = data.currentStreak
                _bestStreak.value = data.bestStreak
            }

            Log.d(TAG, "UI actualizada - Racha: ${data.currentStreak}, Mejor: ${data.bestStreak}, Puntos: ${data.totalPoints}")
        }
    }

    private suspend fun saveStreakData() {
        try {
            streakData?.let { data ->
                val streakInfo = hashMapOf<String, Any>(
                    "currentStreak" to data.currentStreak,
                    "bestStreak" to data.bestStreak,
                    "streakPoints" to data.streakPoints,
                    "totalPoints" to data.totalPoints,
                    "lastUpdate" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    "updatedAt" to System.currentTimeMillis()
                )

                db.collection(COLLECTION_USERS)
                    .document(userId!!)
                    .collection(COLLECTION_RACHA_DATA)
                    .document("current")
                    .set(streakInfo)
                    .await()

                Log.d(TAG, "Datos de racha guardados en Firebase")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar datos de racha: ${e.message}", e)
        }
    }

    private suspend fun createInitialStreakData() {
        try {
            val initialData = hashMapOf<String, Any>(
                "currentStreak" to 0,
                "bestStreak" to 0,
                "streakPoints" to 0,
                "totalPoints" to 0,
                "lastUpdate" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )

            db.collection(COLLECTION_USERS)
                .document(userId!!)
                .collection(COLLECTION_RACHA_DATA)
                .document("current")
                .set(initialData)
                .await()

            Log.d(TAG, "Datos iniciales de racha creados")

        } catch (e: Exception) {
            Log.e(TAG, "Error al crear datos iniciales de racha: ${e.message}", e)
        }
    }

    private fun getStreakLevel(streak: Int): String {
        return when {
            streak >= 30 -> "Nivel: Maestro 👑"
            streak >= 20 -> "Nivel: Experto 🏆"
            streak >= 10 -> "Nivel: Avanzado ⭐"
            streak >= 5 -> "Nivel: Intermedio 🔥"
            else -> "Nivel: Principiante 🌱"
        }
    }

    private fun calculateNextMilestone(currentStreak: Int): MilestoneInfo {
        return when {
            currentStreak < 7 -> {
                val progress = (currentStreak * 100) / 7
                MilestoneInfo(
                    name = "Semana Perfecta",
                    progress = progress,
                    daysRemaining = 7 - currentStreak,
                    rewardPoints = 100,
                    description = "Faltan ${7 - currentStreak} días para desbloquear +100 puntos 🔥"
                )
            }
            currentStreak < 14 -> {
                val progress = ((currentStreak - 7) * 100) / 7
                MilestoneInfo(
                    name = "Dos Semanas Fuertes",
                    progress = progress,
                    daysRemaining = 14 - currentStreak,
                    rewardPoints = 200,
                    description = "Faltan ${14 - currentStreak} días para desbloquear +200 puntos 💪"
                )
            }
            currentStreak < 30 -> {
                val progress = ((currentStreak - 14) * 100) / 16
                MilestoneInfo(
                    name = "Mes Completo",
                    progress = progress,
                    daysRemaining = 30 - currentStreak,
                    rewardPoints = 500,
                    description = "Faltan ${30 - currentStreak} días para desbloquear +500 puntos 👑"
                )
            }
            else -> {
                // Para rachas más largas, siguiente hito cada 30 días
                val nextMilestone = ((currentStreak / 30) + 1) * 30
                val progress = ((currentStreak % 30) * 100) / 30
                MilestoneInfo(
                    name = "Hito Maestro",
                    progress = progress,
                    daysRemaining = nextMilestone - currentStreak,
                    rewardPoints = 1000,
                    description = "Faltan ${nextMilestone - currentStreak} días para el siguiente hito maestro 🌟"
                )
            }
        }
    }

    private fun getMotivationalMessage(streak: Int): String {
        val messages = when {
            streak >= 30 -> listOf(
                "¡Eres una leyenda! Tu disciplina es admirable 👑",
                "¡Maestro de la constancia! Sigues inspirando 🌟",
                "Tu dedicación rompe todos los límites 🚀"
            )
            streak >= 20 -> listOf(
                "¡Impresionante racha! Tu disciplina inspira 🏆",
                "¡Experto en constancia! Sigue así 💪",
                "Tu dedicación es ejemplar 🌟"
            )
            streak >= 10 -> listOf(
                "¡Excelente progreso! Vas por buen camino ⭐",
                "¡Tu constancia da frutos! Sigue adelante 🔥",
                "¡Avanzando como un campeón! 💯"
            )
            streak >= 5 -> listOf(
                "¡Buen ritmo! La constancia es clave 🔥",
                "¡Vas mejorando cada día! 📈",
                "¡Tu esfuerzo se nota! Continúa así ✨"
            )
            else -> listOf(
                "¡Cada día cuenta! Sigue construyendo tu racha 🌱",
                "¡El primer paso es el más importante! 💚",
                "¡Comienza tu viaje hacia la excelencia! 🚀"
            )
        }
        return messages.random()
    }

    private fun getDailyTip(): String {
        val tips = listOf(
            "Completa tu meta diaria antes de las 6 PM para mejores resultados 🕕",
            "Divide tu meta en pequeñas tareas durante el día 📝",
            "Mantén una rutina consistente para fortalecer tu racha 🔄",
            "Celebra cada día completado, ¡cada uno importa! 🎉",
            "Planifica tu día anterior para asegurar el éxito ⏰",
            "Encuentra un compañero de entrenamiento para más motivación 👥"
        )
        return tips.random()
    }

    fun incrementStreak() {
        viewModelScope.launch {
            try {
                val current = _currentStreak.value ?: 0
                val newStreak = current + 1

                // Calcular puntos por racha
                val streakPointsEarned = calculatePointsForDay(newStreak)
                val currentStreakPoints = streakData?.streakPoints ?: 0

                // Actualizar datos
                streakData?.let { data ->
                    data.currentStreak = newStreak
                    data.streakPoints = currentStreakPoints + streakPointsEarned

                    // Recalcular puntos totales
                    calculateTotalPoints()
                }

                // Actualizar UI
                updateUI()

                // Guardar en Firebase
                saveStreakData()

                Log.d(TAG, "Racha incrementada a: $newStreak, puntos de racha ganados: $streakPointsEarned")

            } catch (e: Exception) {
                Log.e(TAG, "Error al incrementar racha: ${e.message}", e)
                _errorMessage.value = "Error al actualizar racha"
            }
        }
    }

    private fun calculatePointsForDay(streak: Int): Int {
        return when {
            streak >= 30 -> 15 // Maestro
            streak >= 20 -> 12 // Experto
            streak >= 10 -> 10 // Avanzado
            streak >= 5 -> 8   // Intermedio
            else -> 5          // Principiante
        }
    }

    fun refreshData() {
        userId?.let { loadStreakData(it) }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Data classes
    data class MilestoneInfo(
        val name: String,
        val progress: Int,
        val daysRemaining: Int,
        val rewardPoints: Int,
        val description: String
    )

    data class DayStatus(
        val date: String,
        val dayName: String,
        val completed: Boolean
    )

    private data class StreakData(
        var currentStreak: Int = 0,
        var bestStreak: Int = 0,
        var streakPoints: Int = 0, // Puntos específicos por mantener racha
        var totalPoints: Int = 0,  // Puntos de racha + puntos de metas diarias
        var lastUpdate: String = ""
    )
}