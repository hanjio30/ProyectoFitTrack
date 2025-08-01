package com.example.fittrack.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DistanciaRecorridaViewModel : ViewModel() {

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
            // Datos principales
            _distanciaTotal.value = "150.5 km"
            _distanciaSemana.value = "28.5 km"
            _distanciaMes.value = "112.8 km"

            // Progreso semanal
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

            Log.d(TAG, "Datos mock cargados exitosamente")

        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar datos mock: ${e.message}", e)
            _errorMessage.value = "Error al procesar los datos"
        }
    }

    fun refreshData() {
        loadDistanceData(null)
    }

    fun clearError() {
        _errorMessage.value = null
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