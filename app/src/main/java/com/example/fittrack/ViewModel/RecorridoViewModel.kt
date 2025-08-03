package com.example.fittrack.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
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
    val tipoActividad: String = "Running"
)

class RecorridoViewModel : ViewModel() {

    private val _recorridos = MutableLiveData<MutableList<Recorrido>>(mutableListOf())
    val recorridos: LiveData<MutableList<Recorrido>> = _recorridos

    fun agregarRecorrido(
        distanciaKm: Float,
        tiempoMs: Long,
        coordenadasInicio: LatLng?,
        coordenadasFin: LatLng?
    ) {
        val fecha = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
        val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        // Formatear duración
        val hours = tiempoMs / 3600000
        val minutes = (tiempoMs % 3600000) / 60000
        val duracion = when {
            hours > 0 -> "${hours}h ${minutes}min"
            minutes > 0 -> "${minutes} minutos"
            else -> "< 1 minuto"
        }

        // Calcular velocidad promedio
        val tiempoHoras = tiempoMs / 3600000f
        val velocidad = if (tiempoHoras > 0) distanciaKm / tiempoHoras else 0f
        val velocidadPromedio = String.format("%.1f km/h", velocidad)

        val nuevoRecorrido = Recorrido(
            fecha = fecha,
            hora = hora,
            duracion = duracion,
            distancia = String.format("%.2f km", distanciaKm),
            origen = if (coordenadasInicio != null) "Coordenada de inicio" else "Ubicación actual",
            destino = if (coordenadasFin != null) "Coordenada final" else "Destino final",
            coordenadasInicio = coordenadasInicio,
            coordenadasFin = coordenadasFin,
            velocidadPromedio = velocidadPromedio
        )

        val listaActual = _recorridos.value ?: mutableListOf()
        listaActual.add(0, nuevoRecorrido) // Agregar al inicio
        _recorridos.value = listaActual
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

            // Obtener día de la semana siempre
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
            "Lunes"
        }
    }
}