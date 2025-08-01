package com.example.fittrack.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HidratacionViewModel : ViewModel() {

    companion object {
        private const val TAG = "HidratacionViewModel"
    }

    // LiveData para los datos de hidratación
    private val _currentHydration = MutableLiveData<String>()
    val currentHydration: LiveData<String> = _currentHydration

    private val _dailyGoal = MutableLiveData<String>()
    val dailyGoal: LiveData<String> = _dailyGoal

    private val _waterGlassLevel = MutableLiveData<Int>()
    val waterGlassLevel: LiveData<Int> = _waterGlassLevel

    private val _dailyTip = MutableLiveData<String>()
    val dailyTip: LiveData<String> = _dailyTip

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Recordatorios de hidratación
    private val _hydrationReminders = MutableLiveData<List<RecordatorioHidratacion>>()
    val hydrationReminders: LiveData<List<RecordatorioHidratacion>> = _hydrationReminders

    // Estadísticas de hidratación
    private val _hydrationStats = MutableLiveData<EstadisticasHidratacion>()
    val hydrationStats: LiveData<EstadisticasHidratacion> = _hydrationStats

    // Historial semanal
    private val _weeklyHistory = MutableLiveData<List<HidratacionSemanal>>()
    val weeklyHistory: LiveData<List<HidratacionSemanal>> = _weeklyHistory

    fun loadHydrationData(userName: String?) {
        try {
            Log.d(TAG, "Cargando datos de hidratación para: $userName")

            _isLoading.value = true

            // Simular carga de datos
            loadMockData()

            _isLoading.value = false

        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar datos: ${e.message}", e)
            _errorMessage.value = "Error al cargar estadísticas de hidratación"
            _isLoading.value = false
        }
    }

    private fun loadMockData() {
        try {
            // Datos principales
            _currentHydration.value = "1.8 L"
            _dailyGoal.value = "Meta diaria 2.5L"
            _waterGlassLevel.value = 72 // 72% del objetivo diario

            // Tip del día
            _dailyTip.value = "Mantente hidratado para despertar por dentro tu belleza natural. La hidratación facilita el flujo sanguíneo y aporta la lucidez en piel y cuerpo que tanto amas."

            // Recordatorios de hidratación
            val recordatorios = listOf(
                RecordatorioHidratacion(
                    id = 1,
                    hora = "08:00",
                    titulo = "Desayuno",
                    descripcion = "Comienza el día con un vaso de agua",
                    cantidad = "250ml",
                    completado = true,
                    tipo = TipoRecordatorio.MAÑANA
                ),
                RecordatorioHidratacion(
                    id = 2,
                    hora = "10:30",
                    titulo = "Media mañana",
                    descripcion = "Mantén tu energía con hidratación",
                    cantidad = "200ml",
                    completado = true,
                    tipo = TipoRecordatorio.MAÑANA
                ),
                RecordatorioHidratacion(
                    id = 3,
                    hora = "12:00",
                    titulo = "Almuerzo",
                    descripcion = "Acompaña tu comida con agua",
                    cantidad = "300ml",
                    completado = true,
                    tipo = TipoRecordatorio.TARDE
                ),
                RecordatorioHidratacion(
                    id = 4,
                    hora = "15:00",
                    titulo = "Tarde",
                    descripcion = "Recarga de energía e hidratación",
                    cantidad = "250ml",
                    completado = false,
                    tipo = TipoRecordatorio.TARDE
                ),
                RecordatorioHidratacion(
                    id = 5,
                    hora = "18:00",
                    titulo = "Pre-cena",
                    descripcion = "Hidratación antes de cenar",
                    cantidad = "200ml",
                    completado = false,
                    tipo = TipoRecordatorio.NOCHE
                ),
                RecordatorioHidratacion(
                    id = 6,
                    hora = "20:30",
                    titulo = "Cena",
                    descripcion = "Última hidratación del día",
                    cantidad = "250ml",
                    completado = false,
                    tipo = TipoRecordatorio.NOCHE
                )
            )
            _hydrationReminders.value = recordatorios

            // Estadísticas de hidratación
            val stats = EstadisticasHidratacion(
                promedioSemanal = 2.1f,
                mejorDia = "Lunes - 2.8L",
                rachaActual = 5, // días consecutivos
                totalSemana = 14.7f,
                porcentajeObjetivo = 72
            )
            _hydrationStats.value = stats

            // Historial semanal
            val historialSemanal = listOf(
                HidratacionSemanal("Lun", 2.8f, 100),
                HidratacionSemanal("Mar", 2.2f, 88),
                HidratacionSemanal("Mié", 1.9f, 76),
                HidratacionSemanal("Jue", 2.5f, 100),
                HidratacionSemanal("Vie", 2.1f, 84),
                HidratacionSemanal("Sáb", 1.8f, 72),
                HidratacionSemanal("Dom", 1.4f, 56)
            )
            _weeklyHistory.value = historialSemanal

            Log.d(TAG, "Datos mock de hidratación cargados exitosamente")

        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar datos mock: ${e.message}", e)
            _errorMessage.value = "Error al procesar los datos"
        }
    }

    fun addWaterIntake(amount: Float) {
        try {
            Log.d(TAG, "Agregando ${amount}L de agua")

            val currentAmount = getCurrentHydrationAmount()
            val newAmount = currentAmount + amount

            _currentHydration.value = "${String.format("%.1f", newAmount)} L"

            // Actualizar nivel del vaso
            val percentage = ((newAmount / 2.5f) * 100).toInt().coerceAtMost(100)
            _waterGlassLevel.value = percentage

            Log.d(TAG, "Nueva cantidad: ${newAmount}L, Porcentaje: $percentage%")

        } catch (e: Exception) {
            Log.e(TAG, "Error al agregar agua: ${e.message}", e)
            _errorMessage.value = "Error al registrar hidratación"
        }
    }

    fun completeReminder(reminderId: Int) {
        try {
            val currentReminders = _hydrationReminders.value?.toMutableList() ?: return
            val reminderIndex = currentReminders.indexOfFirst { it.id == reminderId }

            if (reminderIndex != -1) {
                val updatedReminder = currentReminders[reminderIndex].copy(completado = true)
                currentReminders[reminderIndex] = updatedReminder
                _hydrationReminders.value = currentReminders

                // Agregar la cantidad de agua del recordatorio
                val amount = extractAmountFromString(updatedReminder.cantidad)
                addWaterIntake(amount / 1000f) // Convertir ml a litros

                Log.d(TAG, "Recordatorio $reminderId completado")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al completar recordatorio: ${e.message}", e)
            _errorMessage.value = "Error al completar recordatorio"
        }
    }

    private fun getCurrentHydrationAmount(): Float {
        return try {
            val currentText = _currentHydration.value ?: "0.0 L"
            currentText.replace(" L", "").toFloat()
        } catch (e: Exception) {
            0.0f
        }
    }

    private fun extractAmountFromString(cantidadStr: String): Float {
        return try {
            cantidadStr.replace("ml", "").trim().toFloat()
        } catch (e: Exception) {
            250f // valor por defecto
        }
    }

    fun refreshData() {
        loadHydrationData(null)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun resetDailyProgress() {
        try {
            _currentHydration.value = "0.0 L"
            _waterGlassLevel.value = 0

            // Resetear recordatorios
            val resetReminders = _hydrationReminders.value?.map { it.copy(completado = false) }
            _hydrationReminders.value = resetReminders ?: emptyList()

            Log.d(TAG, "Progreso diario reseteado")
        } catch (e: Exception) {
            Log.e(TAG, "Error al resetear progreso: ${e.message}", e)
        }
    }

    // Data classes para estructurar los datos
    data class RecordatorioHidratacion(
        val id: Int,
        val hora: String,
        val titulo: String,
        val descripcion: String,
        val cantidad: String,
        val completado: Boolean,
        val tipo: TipoRecordatorio
    )

    enum class TipoRecordatorio {
        MAÑANA, TARDE, NOCHE
    }

    data class EstadisticasHidratacion(
        val promedioSemanal: Float,
        val mejorDia: String,
        val rachaActual: Int,
        val totalSemana: Float,
        val porcentajeObjetivo: Int
    )

    data class HidratacionSemanal(
        val dia: String,
        val cantidad: Float,
        val porcentaje: Int
    )
}