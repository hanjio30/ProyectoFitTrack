package com.example.fittrack.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RachaDiariaViewModel : ViewModel() {

    companion object {
        private const val TAG = "RachaDiariaViewModel"
    }

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

    private val _streakStatistics = MutableLiveData<StreakStatistics>()
    val streakStatistics: LiveData<StreakStatistics> = _streakStatistics

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadStreakData(userName: String?) {
        try {
            Log.d(TAG, "Cargando datos de racha para: $userName")

            _isLoading.value = true

            // Simular carga de datos
            loadMockData()

            _isLoading.value = false

        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar datos: ${e.message}", e)
            _errorMessage.value = "Error al cargar estadísticas de racha"
            _isLoading.value = false
        }
    }

    private fun loadMockData() {
        try {
            // Datos principales de racha
            _currentStreak.value = 22
            _streakLevel.value = "Nivel: Experto 🏆"
            _bestStreak.value = 45
            _totalPoints.value = 230

            // Información del próximo hito
            val milestoneInfo = MilestoneInfo(
                name = "Mes Completo",
                progress = 70,
                daysRemaining = 8,
                rewardPoints = 500,
                description = "Faltan 8 días para desbloquear +500 puntos 🎁"
            )
            _nextMilestone.value = milestoneInfo

            // Mensajes motivacionales
            _motivationalMessage.value = "¡Impresionante racha! Tu disciplina inspira."
            _dailyTip.value = "Completa tu meta diaria para mantener tu racha activa."

            // Estadísticas detalladas
            val stats = StreakStatistics(
                currentStreak = 22,
                bestStreak = 45,
                totalPoints = 230,
                level = "Experto",
                completionRate = 92.5f,
                averageDaily = 85.2f,
                weeklyAverage = 6.2f
            )
            _streakStatistics.value = stats

            Log.d(TAG, "Datos mock de racha cargados exitosamente")

        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar datos mock: ${e.message}", e)
            _errorMessage.value = "Error al procesar los datos"
        }
    }

    fun incrementStreak() {
        try {
            val current = _currentStreak.value ?: 0
            val newStreak = current + 1

            _currentStreak.value = newStreak

            // Actualizar nivel si es necesario
            updateStreakLevel(newStreak)

            // Actualizar puntos
            val currentPoints = _totalPoints.value ?: 0
            _totalPoints.value = currentPoints + calculatePointsForDay(newStreak)

            Log.d(TAG, "Racha incrementada a: $newStreak")

        } catch (e: Exception) {
            Log.e(TAG, "Error al incrementar racha: ${e.message}", e)
            _errorMessage.value = "Error al actualizar racha"
        }
    }

    fun resetStreak() {
        try {
            _currentStreak.value = 0
            _streakLevel.value = "Nivel: Principiante 🌱"

            Log.d(TAG, "Racha reseteada")

        } catch (e: Exception) {
            Log.e(TAG, "Error al resetear racha: ${e.message}", e)
        }
    }

    private fun updateStreakLevel(streak: Int) {
        val level = when {
            streak >= 30 -> "Nivel: Maestro 👑"
            streak >= 20 -> "Nivel: Experto 🏆"
            streak >= 10 -> "Nivel: Avanzado ⭐"
            streak >= 5 -> "Nivel: Intermedio 🔥"
            else -> "Nivel: Principiante 🌱"
        }
        _streakLevel.value = level
    }

    private fun calculatePointsForDay(streak: Int): Int {
        return when {
            streak >= 30 -> 15 // Maestro
            streak >= 20 -> 12 // Experto
            streak >= 10 -> 10 // Avanzado
            streak >= 5 -> 8  // Intermedio
            else -> 5         // Principiante
        }
    }

    fun updateMilestoneProgress() {
        try {
            val current = _nextMilestone.value ?: return
            val newProgress = minOf(current.progress + 3, 100) // Incrementar 3% cada día
            val daysRemaining = maxOf(current.daysRemaining - 1, 0)

            val updatedMilestone = current.copy(
                progress = newProgress,
                daysRemaining = daysRemaining,
                description = if (daysRemaining > 0)
                    "Faltan $daysRemaining días para desbloquear +${current.rewardPoints} puntos 🎁"
                else
                    "¡Hito completado! +${current.rewardPoints} puntos desbloqueados 🎉"
            )

            _nextMilestone.value = updatedMilestone

        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar hito: ${e.message}", e)
        }
    }

    fun refreshData() {
        loadStreakData(null)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Data classes para estructurar los datos
    data class MilestoneInfo(
        val name: String,
        val progress: Int,
        val daysRemaining: Int,
        val rewardPoints: Int,
        val description: String
    )

    data class StreakStatistics(
        val currentStreak: Int,
        val bestStreak: Int,
        val totalPoints: Int,
        val level: String,
        val completionRate: Float,
        val averageDaily: Float,
        val weeklyAverage: Float
    )
}