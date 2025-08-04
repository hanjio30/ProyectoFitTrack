package com.example.fittrack.network

import android.util.Log
import com.example.fittrack.ViewModel.Recorrido
import com.example.fittrack.network.Callback
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class RecorridoRepository {

    private val db = FirebaseFirestore.getInstance()
    private val metaDiariaRepository = MetaDiariaRepository() // ✨ INTEGRACIÓN CON METAS
    private val TAG = "RecorridoRepository"

    // ✅ DATA CLASS PARA ESTADÍSTICAS DIARIAS
    data class EstadisticasDiarias(
        val totalCalorias: Int = 0,
        val totalMinutos: Int = 0,
        val totalPasos: Int = 0,
        val totalDistanciaKm: Double = 0.0,
        val totalRecorridos: Int = 0
    )

    // ✨ FUNCIÓN PRINCIPAL RESTAURADA: Con parámetro pasosReales + integración de metas
    fun guardarRecorrido(
        userId: String,
        distanciaKm: Float,
        tiempoMs: Long,
        coordenadasInicio: LatLng?,
        coordenadasFin: LatLng?,
        pasosReales: Int = -1, // ✅ RESTAURADO: Parámetro para pasos reales
        tipoActividad: String = "Caminata",
        notas: String = "",
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

            // ✅ RESTAURADO: Usar pasos reales si están disponibles, sino estimar
            val pasos = if (pasosReales > 0) {
                pasosReales
            } else {
                (distanciaKm * 1300).toInt() // Estimación como fallback
            }

            // ✅ RESTAURADO: Indicar si los pasos son reales o estimados
            val pasosMetadata = if (pasosReales > 0) "real" else "estimado"

            // ✅ RESTAURADO: Calcular calorías más precisas basadas en pasos reales si están disponibles
            val caloriasQuemadas = if (pasosReales > 0) {
                // Fórmula más precisa: aproximadamente 0.04 calorías por paso
                (pasosReales * 0.04).toInt()
            } else {
                // Estimación básica por distancia
                (distanciaKm * 60).toInt()
            }

            // Crear documento del recorrido
            val recorridoData = hashMapOf(
                "id" to UUID.randomUUID().toString(),
                "userId" to userId,
                "fecha" to fecha,
                "fechaFormateada" to fechaFormateada,
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
                "pasos" to pasos,
                "pasosMetadata" to pasosMetadata, // ✅ RESTAURADO: "real" o "estimado"
                "caloriasQuemadas" to caloriasQuemadas,
                "notas" to notas,
                "imagenBase64" to "",
                "timestamp" to timestamp,
                "createdAt" to timestamp,
                "updatedAt" to timestamp,
                "semana" to obtenerSemanaAnio()
            )

            Log.d(TAG, "Guardando recorrido para usuario: $userId - Distancia: ${distanciaKm}km")
            Log.d(TAG, "Pasos: $pasos ($pasosMetadata)") // ✅ RESTAURADO
            Log.d(TAG, "Datos del recorrido: $recorridoData")

            // Guardar en Firestore
            db.collection("users")
                .document(userId)
                .collection("recorridos")
                .add(recorridoData)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "✅ Recorrido guardado exitosamente con ID: ${documentReference.id}")

                    // Actualizar el documento con su propio ID
                    documentReference.update("id", documentReference.id)
                        .addOnSuccessListener {
                            Log.d(TAG, "ID del documento actualizado correctamente")

                            // ✨ FUNCIONALIDAD DE TU COMPAÑERO: Actualizar meta diaria automáticamente
                            actualizarMetaDiariaAutomaticamente(userId, fecha, distanciaKm.toDouble())

                            callback.onSuccess(true)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error al actualizar ID del documento: ${e.message}", e)

                            // ✨ Aún así actualizar la meta diaria
                            actualizarMetaDiariaAutomaticamente(userId, fecha, distanciaKm.toDouble())

                            callback.onSuccess(true)
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

    // ✨ FUNCIÓN DE TU COMPAÑERO: Actualizar meta diaria automáticamente
    private fun actualizarMetaDiariaAutomaticamente(userId: String, fecha: String, distanciaKm: Double) {
        // Usar Coroutine para no bloquear el hilo principal
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "🎯 Actualizando meta diaria automáticamente...")
                Log.d(TAG, "Usuario: $userId, Fecha: $fecha, Nueva distancia: ${distanciaKm}km")

                val resultado = metaDiariaRepository.actualizarMetaTrasNuevoRecorrido(userId, fecha)

                if (resultado.isSuccess) {
                    val metaActualizada = resultado.getOrNull()!!
                    Log.d(TAG, "✅ Meta diaria actualizada automáticamente!")
                    Log.d(TAG, "📊 Progreso actual: ${metaActualizada.progresoActual}km/${metaActualizada.metaKilometros}km")
                    Log.d(TAG, "📈 Porcentaje: ${metaActualizada.porcentajeCompletado}%")
                    Log.d(TAG, "🏆 Puntos: ${metaActualizada.puntosGanados}")

                    if (metaActualizada.metaAlcanzada) {
                        Log.d(TAG, "🎉 ¡META DIARIA COMPLETADA!")
                    }
                } else {
                    val error = resultado.exceptionOrNull()
                    Log.w(TAG, "⚠ Error al actualizar meta diaria: ${error?.message}", error)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error inesperado al actualizar meta: ${e.message}", e)
            }
        }
    }

    // ✨ FUNCIÓN DE TU COMPAÑERO: Versión con callback para obtener el resultado de la meta
    fun guardarRecorridoConMetaCallback(
        userId: String,
        distanciaKm: Float,
        tiempoMs: Long,
        coordenadasInicio: LatLng?,
        coordenadasFin: LatLng?,
        pasosReales: Int = -1, // ✅ AGREGADO: Parámetro pasosReales también aquí
        tipoActividad: String = "Caminata",
        notas: String = "",
        callback: Callback<Boolean>,
        metaCallback: ((metaCompletada: Boolean, progreso: Double, porcentaje: Int) -> Unit)? = null
    ) {
        try {
            val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // Reutilizar la lógica existente pero con callback adicional
            guardarRecorrido(userId, distanciaKm, tiempoMs, coordenadasInicio, coordenadasFin, pasosReales, tipoActividad, notas,
                object : Callback<Boolean> {
                    override fun onSuccess(result: Boolean?) {
                        // Callback original
                        callback.onSuccess(result)

                        // ✨ Callback adicional para la meta con más detalles
                        metaCallback?.let { metaCb ->
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val resultadoMeta = metaDiariaRepository.getMetaDiariaActual(userId)
                                    if (resultadoMeta.isSuccess) {
                                        val meta = resultadoMeta.getOrNull()!!
                                        CoroutineScope(Dispatchers.Main).launch {
                                            metaCb(meta.metaAlcanzada, meta.progresoActual, meta.porcentajeCompletado)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error al obtener detalles de meta: ${e.message}", e)
                                }
                            }
                        }
                    }

                    override fun onFailed(exception: Exception) {
                        callback.onFailed(exception)
                    }
                })

        } catch (e: Exception) {
            Log.e(TAG, "Error en guardarRecorridoConMetaCallback: ${e.message}", e)
            callback.onFailed(e)
        }
    }

    // ✨ RESTO DE FUNCIONES DE TU COMPAÑERO (sin cambios)
    suspend fun obtenerProgresoDelDia(userId: String): Result<Double> {
        return try {
            val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val documents = db.collection("users")
                .document(userId)
                .collection("recorridos")
                .whereEqualTo("fecha", fecha)
                .get()
                .await()

            var totalDistancia = 0.0
            var contadorRecorridos = 0

            for (document in documents.documents) {
                val distancia = document.getDouble("distanciaKm") ?: 0.0
                totalDistancia += distancia
                contadorRecorridos++
            }

            Log.d(TAG, "📊 Progreso del día ($fecha): ${totalDistancia}km en $contadorRecorridos recorridos")
            Result.success(totalDistancia)

        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener progreso del día: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun recalcularMetaParaFecha(userId: String, fecha: String): Result<Boolean> {
        return try {
            Log.d(TAG, "🔄 Recalculando meta para fecha: $fecha")

            val resultado = metaDiariaRepository.actualizarMetaTrasNuevoRecorrido(userId, fecha)

            if (resultado.isSuccess) {
                Log.d(TAG, "✅ Meta recalculada exitosamente para $fecha")
                Result.success(true)
            } else {
                Log.e(TAG, "❌ Error al recalcular meta para $fecha")
                Result.failure(resultado.exceptionOrNull() ?: Exception("Error desconocido"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al recalcular meta: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun recalcularTodasLasMetas(userId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "🔄 Recalculando todas las metas para usuario: $userId")

            val recorridos = db.collection("users")
                .document(userId)
                .collection("recorridos")
                .get()
                .await()

            val fechasUnicas = mutableSetOf<String>()
            for (documento in recorridos.documents) {
                val fecha = documento.getString("fecha")
                if (!fecha.isNullOrEmpty()) {
                    fechasUnicas.add(fecha)
                }
            }

            Log.d(TAG, "📅 Fechas encontradas para recalcular: ${fechasUnicas.size}")

            var metasActualizadas = 0
            for (fecha in fechasUnicas) {
                try {
                    val resultado = metaDiariaRepository.actualizarMetaTrasNuevoRecorrido(userId, fecha)
                    if (resultado.isSuccess) {
                        metasActualizadas++
                        Log.d(TAG, "✅ Meta actualizada para: $fecha")
                    } else {
                        Log.w(TAG, "⚠ No se pudo actualizar meta para: $fecha")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al actualizar meta para $fecha: ${e.message}", e)
                }
            }

            Log.d(TAG, "🎯 Recálculo completado: $metasActualizadas/${fechasUnicas.size} metas actualizadas")
            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Error al recalcular todas las metas: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun obtenerEstadisticasDiarias(
        userId: String,
        fecha: String? = null
    ): Result<EstadisticasDiarias> {
        return try {
            val fechaBuscar = fecha ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            Log.d(TAG, "Obteniendo estadísticas para fecha: $fechaBuscar")

            val documents = db.collection("users")
                .document(userId)
                .collection("recorridos")
                .whereEqualTo("fecha", fechaBuscar)
                .get()
                .await()

            var totalCalorias = 0
            var totalMinutos = 0
            var totalPasos = 0
            var totalDistancia = 0.0
            var totalRecorridos = 0

            for (document in documents) {
                try {
                    val data = document.data

                    val calorias = (data["caloriasQuemadas"] as? Number)?.toInt() ?: 0
                    val minutos = (data["duracionMinutos"] as? Number)?.toInt() ?: 0
                    val pasos = (data["pasos"] as? Number)?.toInt() ?: 0
                    val distancia = (data["distanciaKm"] as? Number)?.toDouble() ?: 0.0

                    totalCalorias += calorias
                    totalMinutos += minutos
                    totalPasos += pasos
                    totalDistancia += distancia
                    totalRecorridos++

                    Log.d(
                        TAG,
                        "Recorrido ${document.id}: ${calorias}cal, ${minutos}min, ${pasos}pasos, ${distancia}km"
                    )

                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Error al procesar estadísticas del documento ${document.id}: ${e.message}",
                        e
                    )
                }
            }

            val estadisticas = EstadisticasDiarias(
                totalCalorias = totalCalorias,
                totalMinutos = totalMinutos,
                totalPasos = totalPasos,
                totalDistanciaKm = totalDistancia,
                totalRecorridos = totalRecorridos
            )

            Log.d(TAG, "✓ Estadísticas calculadas: $estadisticas")
            Result.success(estadisticas)

        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener estadísticas diarias: ${e.message}", e)
            Result.failure(e)
        }
    }

    // 📋 FUNCIONES EXISTENTES (sin cambios)

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