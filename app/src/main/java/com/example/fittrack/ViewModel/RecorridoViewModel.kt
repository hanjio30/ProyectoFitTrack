package com.example.fittrack.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fittrack.network.Callback
import com.example.fittrack.network.RecorridoRepository
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class Recorrido(
    val id: String = UUID.randomUUID().toString(),
    val fecha: String,
    val hora: String,
    val duracion: String,
    val distancia: String,
    val origen: String,
    val destino: String,
    val coordenadasInicio: LatLng?,
    val coordenadasFin: LatLng?,
    val velocidadPromedio: String,
    val tipoActividad: String = "Caminata"
)

class RecorridoViewModel : ViewModel() {

    private val recorridoRepository = RecorridoRepository()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val TAG = "RecorridoViewModel"

    private val _recorridos = MutableLiveData<MutableList<Recorrido>>(mutableListOf())
    val recorridos: LiveData<MutableList<Recorrido>> = _recorridos

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _guardadoExitoso = MutableLiveData<Boolean>()
    val guardadoExitoso: LiveData<Boolean> = _guardadoExitoso

    init {
        cargarRecorridos()
    }

    fun agregarRecorrido(
        distanciaKm: Float,
        tiempoMs: Long,
        coordenadasInicio: LatLng?,
        coordenadasFin: LatLng?,
        tipoActividad: String = "Caminata", // Opcional, por defecto "Caminata"
        notas: String = "" // Opcional, por defecto vacío
    ) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _error.value = "Usuario no autenticado"
            return
        }

        _isLoading.value = true
        _error.value = null

        Log.d(TAG, "Iniciando guardado de recorrido para usuario: $userId")
        Log.d(TAG, "Distancia: $distanciaKm km, Tiempo: $tiempoMs ms")

        recorridoRepository.guardarRecorrido(
            userId = userId,
            distanciaKm = distanciaKm,
            tiempoMs = tiempoMs,
            coordenadasInicio = coordenadasInicio,
            coordenadasFin = coordenadasFin,
            tipoActividad = tipoActividad,
            notas = notas,
            callback = object : Callback<Boolean> {
                override fun onSuccess(result: Boolean?) {
                    Log.d(TAG, "Recorrido guardado exitosamente")
                    _isLoading.value = false
                    _guardadoExitoso.value = true

                    // Recargar la lista de recorridos
                    cargarRecorridos()
                }

                override fun onFailed(exception: Exception) {
                    Log.e(TAG, "Error al guardar recorrido: ${exception.message}", exception)
                    _isLoading.value = false
                    _error.value = "Error al guardar recorrido: ${exception.message}"
                    _guardadoExitoso.value = false
                }
            }
        )
    }

    fun cargarRecorridos() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _error.value = "Usuario no autenticado"
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val result = recorridoRepository.obtenerRecorridos(userId)

                result.onSuccess { listaRecorridos ->
                    Log.d(TAG, "Recorridos cargados exitosamente: ${listaRecorridos.size}")
                    _recorridos.value = listaRecorridos.toMutableList()
                    _isLoading.value = false
                }

                result.onFailure { exception ->
                    Log.e(TAG, "Error al cargar recorridos: ${exception.message}", exception)
                    _error.value = "Error al cargar recorridos: ${exception.message}"
                    _isLoading.value = false
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado al cargar recorridos: ${e.message}", e)
                _error.value = "Error inesperado: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun obtenerRecorridosPorDia(): Map<String, List<Recorrido>> {
        val recorridosList = _recorridos.value ?: return emptyMap()
        return recorridosList.groupBy { obtenerDiaSemana(it.fecha) }
    }

    private fun obtenerDiaSemana(fecha: String): String {
        return try {
            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            val fechaRecorrido = sdf.parse(fecha)
            val calRecorrido = Calendar.getInstance().apply { time = fechaRecorrido }

            when (calRecorrido.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Lunes"
                Calendar.TUESDAY -> "Martes"
                Calendar.WEDNESDAY -> "Miércoles"
                Calendar.THURSDAY -> "Jueves"
                Calendar.FRIDAY -> "Viernes"
                Calendar.SATURDAY -> "Sábado"
                Calendar.SUNDAY -> "Domingo"
                else -> "Lunes"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener día de la semana: ${e.message}", e)
            "Lunes"
        }
    }

    // Limpiar mensajes de error
    fun clearError() {
        _error.value = null
    }

    // Limpiar estado de guardado exitoso
    fun clearGuardadoExitoso() {
        _guardadoExitoso.value = false
    }

    // Agregar datos de prueba (solo para desarrollo)
    fun agregarDatosPrueba() {
        Log.d(TAG, "Agregando datos de prueba...")

        // Simulamos un recorrido de prueba
        agregarRecorrido(
            distanciaKm = 2.5f,
            tiempoMs = 45 * 60 * 1000L, // 45 minutos
            coordenadasInicio = LatLng(-9.535, -77.024), // Coordenadas de ejemplo
            coordenadasFin = LatLng(-9.540, -77.030)
        )
    }
}