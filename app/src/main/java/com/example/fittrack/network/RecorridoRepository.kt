package com.example.fittrack.network

import android.util.Log
import com.example.fittrack.ViewModel.Recorrido
import com.example.fittrack.network.Callback
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class RecorridoRepository {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "RecorridoRepository"

    // Guardar recorrido en Firestore
    fun guardarRecorrido(
        userId: String,
        distanciaKm: Float,
        tiempoMs: Long,
        coordenadasInicio: LatLng?,
        coordenadasFin: LatLng?,
        tipoActividad: String = "Caminata", // Parámetro opcional
        notas: String = "", // Parámetro opcional
        callback: Callback<Boolean>
    ) {
        try {
            val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val fechaFormateada = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
            val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val diaSemana = obtenerDiaSemana()
            val timestamp = System.currentTimeMillis()

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

            // Calcular pasos estimados (aproximado: 1300 pasos por km)
            val pasosEstimados = (distanciaKm * 1300).toInt()

            // Calcular calorías quemadas básico (aproximado: 60 cal por km para persona promedio)
            // Puedes hacer esto más sofisticado más adelante con peso del usuario
            val caloriasQuemadas = (distanciaKm * 60).toInt()

            // Crear documento del recorrido
            val recorridoData = hashMapOf(
                "id" to UUID.randomUUID().toString(),
                "userId" to userId,
                "fecha" to fecha, // Para consultas (yyyy-MM-dd)
                "fechaFormateada" to fechaFormateada, // Para mostrar (dd MMMM yyyy)
                "hora" to hora,
                "diaSemana" to diaSemana,
                "duracionMinutos" to (tiempoMs / 60000).toInt(),
                "duracionTexto" to duracion,
                "distanciaKm" to distanciaKm.toDouble(),
                "velocidadPromedio" to velocidad.toDouble(),
                "coordenadasInicio" to if (coordenadasInicio != null) {
                    hashMapOf(
                        "latitude" to coordenadasInicio.latitude,
                        "longitude" to coordenadasInicio.longitude
                    )
                } else null,
                "coordenadasFin" to if (coordenadasFin != null) {
                    hashMapOf(
                        "latitude" to coordenadasFin.latitude,
                        "longitude" to coordenadasFin.longitude
                    )
                } else null,
                "origen" to if (coordenadasInicio != null) "Coordenada de inicio" else "Ubicación actual",
                "destino" to if (coordenadasFin != null) "Coordenada final" else "Destino final",
                "tipoActividad" to tipoActividad,
                "pasos" to pasosEstimados,
                "caloriasQuemadas" to caloriasQuemadas,
                "notas" to notas, // Campo para notas opcionales (vacío por defecto)
                "imagenBase64" to "", // Campo para imagen opcional (vacío por defecto)
                "timestamp" to timestamp,
                "createdAt" to timestamp,
                "updatedAt" to timestamp,
                "semana" to obtenerSemanaAnio()
            )

            Log.d(TAG, "Guardando recorrido para usuario: $userId")
            Log.d(TAG, "Datos del recorrido: $recorridoData")

            // Guardar en Firestore
            db.collection("users")
                .document(userId)
                .collection("recorridos")
                .add(recorridoData)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "Recorrido guardado exitosamente con ID: ${documentReference.id}")

                    // Actualizar el documento con su propio ID
                    documentReference.update("id", documentReference.id)
                        .addOnSuccessListener {
                            Log.d(TAG, "ID del documento actualizado correctamente")
                            callback.onSuccess(true)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error al actualizar ID del documento: ${e.message}", e)
                            callback.onSuccess(true) // Aún así consideramos exitoso
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al guardar recorrido: ${e.message}", e)
                    callback.onFailed(Exception("Error al guardar recorrido: ${e.message}"))
                }

        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al guardar recorrido: ${e.message}", e)
            callback.onFailed(e)
        }
    }

    // Obtener recorridos del usuario
    suspend fun obtenerRecorridos(userId: String): Result<List<Recorrido>> {
        return try {
            val documents = db.collection("users")
                .document(userId)
                .collection("recorridos")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val recorridos = mutableListOf<Recorrido>()

            for (document in documents) {
                try {
                    val data = document.data

                    // Convertir coordenadas de Maps
                    val coordenadasInicio = data["coordenadasInicio"]?.let { coords ->
                        if (coords is Map<*, *>) {
                            val lat = coords["latitude"] as? Double
                            val lng = coords["longitude"] as? Double
                            if (lat != null && lng != null) LatLng(lat, lng) else null
                        } else null
                    }

                    val coordenadasFin = data["coordenadasFin"]?.let { coords ->
                        if (coords is Map<*, *>) {
                            val lat = coords["latitude"] as? Double
                            val lng = coords["longitude"] as? Double
                            if (lat != null && lng != null) LatLng(lat, lng) else null
                        } else null
                    }

                    val recorrido = Recorrido(
                        id = data["id"] as? String ?: document.id,
                        fecha = data["fechaFormateada"] as? String ?: "",
                        hora = data["hora"] as? String ?: "",
                        duracion = data["duracionTexto"] as? String ?: "",
                        distancia = String.format("%.2f km", (data["distanciaKm"] as? Double) ?: 0.0),
                        origen = data["origen"] as? String ?: "Ubicación actual",
                        destino = data["destino"] as? String ?: "Destino final",
                        coordenadasInicio = coordenadasInicio,
                        coordenadasFin = coordenadasFin,
                        velocidadPromedio = String.format("%.1f km/h", (data["velocidadPromedio"] as? Double) ?: 0.0),
                        tipoActividad = data["tipoActividad"] as? String ?: "Caminata"
                    )

                    recorridos.add(recorrido)
                } catch (e: Exception) {
                    Log.e(TAG, "Error al procesar documento ${document.id}: ${e.message}", e)
                }
            }

            Log.d(TAG, "Recorridos obtenidos: ${recorridos.size}")
            Result.success(recorridos)

        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener recorridos: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Obtener recorridos por fecha específica
    suspend fun obtenerRecorridosPorFecha(userId: String, fecha: String): Result<List<Recorrido>> {
        return try {
            val documents = db.collection("users")
                .document(userId)
                .collection("recorridos")
                .whereEqualTo("fecha", fecha)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val recorridos = documents.mapNotNull { document ->
                try {
                    val data = document.data

                    val coordenadasInicio = data["coordenadasInicio"]?.let { coords ->
                        if (coords is Map<*, *>) {
                            val lat = coords["latitude"] as? Double
                            val lng = coords["longitude"] as? Double
                            if (lat != null && lng != null) LatLng(lat, lng) else null
                        } else null
                    }

                    val coordenadasFin = data["coordenadasFin"]?.let { coords ->
                        if (coords is Map<*, *>) {
                            val lat = coords["latitude"] as? Double
                            val lng = coords["longitude"] as? Double
                            if (lat != null && lng != null) LatLng(lat, lng) else null
                        } else null
                    }

                    Recorrido(
                        id = data["id"] as? String ?: document.id,
                        fecha = data["fechaFormateada"] as? String ?: "",
                        hora = data["hora"] as? String ?: "",
                        duracion = data["duracionTexto"] as? String ?: "",
                        distancia = String.format("%.2f km", (data["distanciaKm"] as? Double) ?: 0.0),
                        origen = data["origen"] as? String ?: "Ubicación actual",
                        destino = data["destino"] as? String ?: "Destino final",
                        coordenadasInicio = coordenadasInicio,
                        coordenadasFin = coordenadasFin,
                        velocidadPromedio = String.format("%.1f km/h", (data["velocidadPromedio"] as? Double) ?: 0.0),
                        tipoActividad = data["tipoActividad"] as? String ?: "Caminata"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error al procesar documento: ${e.message}", e)
                    null
                }
            }

            Result.success(recorridos)
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener recorridos por fecha: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Funciones auxiliares
    private fun obtenerDiaSemana(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Lunes"
            Calendar.TUESDAY -> "Martes"
            Calendar.WEDNESDAY -> "Miércoles"
            Calendar.THURSDAY -> "Jueves"
            Calendar.FRIDAY -> "Viernes"
            Calendar.SATURDAY -> "Sábado"
            Calendar.SUNDAY -> "Domingo"
            else -> "Lunes"
        }
    }

    private fun obtenerSemanaAnio(): String {
        val calendar = Calendar.getInstance()
        val año = calendar.get(Calendar.YEAR)
        val semana = calendar.get(Calendar.WEEK_OF_YEAR)
        return "$año-W${String.format("%02d", semana)}"
    }
}