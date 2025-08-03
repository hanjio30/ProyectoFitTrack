package com.example.fittrack.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModelProvider

class DistanciaRecorridaViewModel(private val context: Context) : ViewModel() {

    companion object {
        private const val TAG = "DistanciaRecorridaViewModel"
    }

    // LiveData para los datos de distancia
    private val _distanciaTotal = MutableLiveData<String>()
    val distanciaTotal: LiveData<String> = _distanciaTotal

    private val _distanciaSemana = MutableLiveData<String>()
    val distanciaSemana: LiveData<String> = _distanciaSemana

    private val _distanciaMes = MutableLiveData<String>()
    val distanciaMes: LiveData<String> = _distanciaMes

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Datos de progreso semanal
    private val _progresoSemanal = MutableLiveData<List<ProgresoSemana>>()
    val progresoSemanal: LiveData<List<ProgresoSemana>> = _progresoSemanal

    // Récords personales
    private val _records = MutableLiveData<RecordsPersonales>()
    val records: LiveData<RecordsPersonales> = _records



    fun loadDistanceData(userName: String?) {
        try {
            Log.d(TAG, "Cargando datos de distancia para: $userName")

            _isLoading.value = true

            // Simular carga de datos
            loadMockData()

            _isLoading.value = false

        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar datos: ${e.message}", e)
            _errorMessage.value = "Error al cargar estadísticas de distancia"
            _isLoading.value = false
        }
    }

    private fun loadMockData() {
        try {
            // Cargar datos desde storage o usar valores por defecto
            val totalDistance = getStoredValue("total_distance", 150.5f)
            val weekDistance = getStoredValue("week_distance", 28.5f)
            val monthDistance = getStoredValue("month_distance", 112.8f)

            // Datos principales
            _distanciaTotal.value = String.format("%.1f km", totalDistance)
            _distanciaSemana.value = String.format("%.1f km", weekDistance)
            _distanciaMes.value = String.format("%.1f km", monthDistance)

            // Progreso semanal (estos pueden seguir siendo mock data)
            val progresoSemanas = listOf(
                ProgresoSemana("Sem 2", 18.5f, 45),
                ProgresoSemana("Sem 3", 22.3f, 55),
                ProgresoSemana("Sem 4", 24.2f, 60),
                ProgresoSemana("Sem 5", 28.5f, 70),
                ProgresoSemana("Sem 6", 31.2f, 77),
                ProgresoSemana("Sem 7", 26.8f, 65),
                ProgresoSemana("Sem 8", 28.5f, 70)
            )
            _progresoSemanal.value = progresoSemanas

            // Récords personales
            val records = RecordsPersonales(
                mejorDia = Record("12 Jun", "15.2 km"),
                mejorSemana = Record("5-11 Jun", "45.8 km"),
                mejorMes = Record("Mayo 2025", "156.3 km")
            )
            _records.value = records

            Log.d(TAG, "Datos cargados exitosamente desde storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar datos: ${e.message}", e)
            _errorMessage.value = "Error al procesar los datos"
        }
    }

    fun refreshData() {
        loadDistanceData(null)
    }

    fun clearError() {
        _errorMessage.value = null
    }
    // Función para agregar nueva distancia
    fun addNewDistance(distance: Float, duration: Long, date: String) {
        try {
            Log.d(TAG, "Agregando nueva distancia: $distance km")

            val currentTotal = getCurrentTotalDistance() + distance
            val currentWeek = getCurrentWeekDistance() + distance
            val currentMonth = getCurrentMonthDistance() + distance

            // Actualizar los LiveData
            _distanciaTotal.value = String.format("%.1f km", currentTotal)
            _distanciaSemana.value = String.format("%.1f km", currentWeek)
            _distanciaMes.value = String.format("%.1f km", currentMonth)

            // Guardar en almacenamiento persistente
            saveToStorage(currentTotal, currentWeek, currentMonth)

            Log.d(TAG, "Distancia agregada exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar distancia: ${e.message}", e)
            _errorMessage.value = "Error al guardar distancia: ${e.message}"
        }
    }

    private fun getCurrentTotalDistance(): Float {
        return getStoredValue("total_distance", 150.5f) // 150.5f es el valor por defecto
    }

    private fun getCurrentWeekDistance(): Float {
        return getStoredValue("week_distance", 28.5f) // 28.5f es el valor por defecto
    }

    private fun getCurrentMonthDistance(): Float {
        return getStoredValue("month_distance", 112.8f) // 112.8f es el valor por defecto
    }

    private fun saveToStorage(total: Float, week: Float, month: Float) {
        try {
            val sharedPref = context.getSharedPreferences("fitness_data", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putFloat("total_distance", total)
                putFloat("week_distance", week)
                putFloat("month_distance", month)
                apply()
            }
            Log.d(TAG, "Datos guardados en storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar en storage: ${e.message}", e)
        }
    }

    private fun getStoredValue(key: String, defaultValue: Float): Float {
        return try {
            val sharedPref = context.getSharedPreferences("fitness_data", Context.MODE_PRIVATE)
            sharedPref.getFloat(key, defaultValue)
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener valor stored: ${e.message}", e)
            defaultValue
        }
    }

    // Data classes para estructurar los datos
    data class ProgresoSemana(
        val nombre: String,
        val distancia: Float,
        val progreso: Int // Porcentaje 0-100
    )

    data class Record(
        val fecha: String,
        val valor: String
    )

    data class RecordsPersonales(
        val mejorDia: Record,
        val mejorSemana: Record,
        val mejorMes: Record
    )
}