package com.example.fittrack.ViewModel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class DistanciaRecorridaViewModel(application: Application) : AndroidViewModel(application) {

    // LiveData para los datos de distancia
    private val _distanciaTotal = MutableLiveData<String>()
    val distanciaTotal: LiveData<String> = _distanciaTotal

    private val _distanciaSemana = MutableLiveData<String>()
    val distanciaSemana: LiveData<String> = _distanciaSemana

    private val _distanciaMes = MutableLiveData<String>()
    val distanciaMes: LiveData<String> = _distanciaMes

    // LiveData para el progreso semanal
    private val _progresoSemanal = MutableLiveData<List<ProgressData>>()
    val progresoSemanal: LiveData<List<ProgressData>> = _progresoSemanal

    // LiveData para récords personales
    private val _recordsPersonales = MutableLiveData<RecordsData>()
    val recordsPersonales: LiveData<RecordsData> = _recordsPersonales

    // LiveData para controlar la visibilidad de elementos de UI
    private val _shouldHideMainUI = MutableLiveData<Boolean>()
    val shouldHideMainUI: LiveData<Boolean> = _shouldHideMainUI

    data class ProgressData(
        val semana: String,
        val distancia: String,
        val progreso: Int
    )

    data class RecordsData(
        val mejorDia: String,
        val fechaMejorDia: String,
        val distanciaMejorDia: String,
        val mejorSemana: String,
        val fechaMejorSemana: String,
        val distanciaMejorSemana: String,
        val mejorMes: String,
        val fechaMejorMes: String,
        val distanciaMejorMes: String
    )

    fun loadDistanceData(sharedPreferences: SharedPreferences) {
        // Aquí cargarías los datos reales desde SharedPreferences, Room, API, etc.
        // Por ahora uso datos de ejemplo

        _distanciaTotal.value = "150.5 km"
        _distanciaSemana.value = "28.5 km"
        _distanciaMes.value = "112.8 km"

        // Cargar progreso semanal
        val progressList = listOf(
            ProgressData("Sem 2", "18.5 km", 45),
            ProgressData("Sem 3", "22.3 km", 55),
            ProgressData("Sem 4", "24.2 km", 60),
            ProgressData("Sem 5", "28.5 km", 70),
            ProgressData("Sem 6", "31.2 km", 77),
            ProgressData("Sem 7", "26.8 km", 65),
            ProgressData("Sem 8", "28.5 km", 70)
        )
        _progresoSemanal.value = progressList

        // Cargar récords personales
        val records = RecordsData(
            mejorDia = "Mejor día",
            fechaMejorDia = "12 Jun",
            distanciaMejorDia = "15.2 km",
            mejorSemana = "Mejor semana",
            fechaMejorSemana = "5-11 Jun",
            distanciaMejorSemana = "45.8 km",
            mejorMes = "Mejor mes",
            fechaMejorMes = "Mayo 2025",
            distanciaMejorMes = "156.3 km"
        )
        _recordsPersonales.value = records
    }

    fun setUIVisibility(shouldHide: Boolean) {
        _shouldHideMainUI.value = shouldHide
    }

    // Método para actualizar datos (si vienen de una API o base de datos)
    fun updateDistanceData(total: String, semana: String, mes: String) {
        _distanciaTotal.value = total
        _distanciaSemana.value = semana
        _distanciaMes.value = mes
    }

    // Método para obtener datos desde Repository (si implementas Repository pattern)
    fun refreshData() {
        // Aquí llamarías a tu Repository para obtener datos actualizados
        // repository.getDistanceData()
    }
}