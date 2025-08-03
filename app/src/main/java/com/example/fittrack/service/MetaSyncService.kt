// MetaSyncService.kt
package com.example.fittrack.service

import android.util.Log
import com.example.fittrack.network.MetaDiariaRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

object MetaSyncService {

    private val TAG = "MetaSyncService"
    private val repository = MetaDiariaRepository()

    /**
     * Actualiza automáticamente el progreso de la meta cuando se agrega un nuevo recorrido
     */
    fun actualizarProgresoMeta(userId: String, distanciaRecorrido: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Actualizando progreso de meta para usuario: $userId, distancia: $distanciaRecorrido")

                val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                // Obtener la meta actual
                val resultMeta = repository.getMetaDiariaActual(userId)

                if (resultMeta.isSuccess) {
                    Log.d(TAG, "Meta actualizada automáticamente por nuevo recorrido")
                } else {
                    Log.e(TAG, "Error al actualizar meta: ${resultMeta.exceptionOrNull()?.message}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error en actualizarProgresoMeta: ${e.message}", e)
            }
        }
    }

    /**
     * Listener para cambios en la colección de recorridos
     */
    fun setupRecorridosListener(userId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(userId)
            .collection("recorridos")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error al escuchar recorridos: ${error.message}", error)
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val recorrido = change.document.data
                        val distancia = recorrido["distanciaKm"] as? Double ?: 0.0

                        // Solo actualizar si es un recorrido de hoy
                        val fechaRecorrido = recorrido["fecha"] as? String
                        val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                        if (fechaRecorrido == fechaHoy) {
                            actualizarProgresoMeta(userId, distancia)
                        }
                    }
                }
            }
    }
}