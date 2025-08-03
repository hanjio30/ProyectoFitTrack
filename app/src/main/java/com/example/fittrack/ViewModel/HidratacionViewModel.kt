package com.example.fittrack.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

class HidratacionViewModel : ViewModel() {

    companion object {
        private const val TAG = "HidratacionViewModel"
    }

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
        Log.d(TAG, "Cargando datos de hidratación para: $userName")

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "Usuario no autenticado")
            _errorMessage.value = "Usuario no autenticado"
            loadMockDataOnly()
            return
        }

        val uid = currentUser.uid
        val fecha = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(Date())
        _isLoading.value = true

        Log.d(TAG, "Cargando hidratación para UID: $uid, Fecha: $fecha")

        // Cargar datos principales de hidratación
        db.collection("users").document(uid).collection("hidratacion").document(fecha)
            .get()
            .addOnSuccessListener { document ->
                try {
                    Log.d(TAG, "Respuesta de Firebase recibida. Documento existe: ${document.exists()}")

                    if (document.exists()) {
                        val litros = document.getDouble("litros") ?: 0.0
                        _currentHydration.value = String.format("%.1f L", litros)
                        val porcentaje = ((litros / 2.5) * 100).toInt().coerceAtMost(100)
                        _waterGlassLevel.value = porcentaje

                        Log.d(TAG, "Datos de Firebase cargados exitosamente: ${litros}L (${porcentaje}%)")
                    } else {
                        _currentHydration.value = "0.0 L"
                        _waterGlassLevel.value = 0
                        Log.d(TAG, "No hay datos en Firebase para hoy, iniciando en 0")
                    }

                    // Cargar recordatorios completados y luego datos mock
                    loadCompletedReminders(uid, fecha)

                } catch (e: Exception) {
                    Log.e(TAG, "Error al procesar datos de Firebase: ${e.message}", e)
                    _errorMessage.value = "Error al procesar datos: ${e.message}"
                    loadMockDataOnly()
                    _isLoading.value = false
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error de conexión con Firestore: ${exception.message}", exception)
                _errorMessage.value = "Error de conexión: ${exception.message}"
                loadMockDataOnly()
                _isLoading.value = false
            }
    }

    fun addWaterIntake(amount: Float) {
        Log.d(TAG, "Agregando ${amount}L de agua")

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "Usuario no autenticado para guardar datos")
            // Aún así actualizar la UI localmente
            updateLocalHydration(amount)
            return
        }

        val uid = currentUser.uid
        val fecha = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(Date())
        val currentAmount = getCurrentHydrationAmount()
        val newAmount = currentAmount + amount

        val data = mapOf(
            "litros" to newAmount,
            "fecha" to fecha,
            "updatedAt" to System.currentTimeMillis()
        )

        Log.d(TAG, "Guardando en Firebase - UID: $uid, Fecha: $fecha, Cantidad: $newAmount")

        // Usar "users" como en PerfilViewModel
        db.collection("users").document(uid).collection("hidratacion").document(fecha)
            .set(data)
            .addOnSuccessListener {
                Log.d(TAG, "Hidratación guardada exitosamente en Firebase: ${newAmount}L")
                updateLocalHydration(amount)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al guardar hidratación en Firebase: ${exception.message}", exception)
                _errorMessage.value = "Error al guardar: ${exception.message}"
                // Aún así actualizar localmente
                updateLocalHydration(amount)
            }
    }

    private fun updateLocalHydration(amount: Float) {
        try {
            val currentAmount = getCurrentHydrationAmount()
            val newAmount = currentAmount + amount

            _currentHydration.value = String.format("%.1f L", newAmount)
            val percentage = ((newAmount / 2.5f) * 100).toInt().coerceAtMost(100)
            _waterGlassLevel.value = percentage

            Log.d(TAG, "UI actualizada: ${newAmount}L, ${percentage}%")
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar UI local: ${e.message}", e)
        }
    }

    fun completeReminder(reminderId: Int) {
        try {
            Log.d(TAG, "Completando recordatorio $reminderId")

            val currentReminders = _hydrationReminders.value?.toMutableList() ?: return
            val reminderIndex = currentReminders.indexOfFirst { it.id == reminderId }

            if (reminderIndex != -1) {
                val updatedReminder = currentReminders[reminderIndex].copy(completado = true)
                currentReminders[reminderIndex] = updatedReminder
                _hydrationReminders.value = currentReminders

                // Agregar la cantidad de agua del recordatorio
                val amount = extractAmountFromString(updatedReminder.cantidad)
                addWaterIntake(amount / 1000f) // Convertir ml a litros

                // Guardar el estado completado en Firebase
                saveReminderCompletedState(reminderId)

                Log.d(TAG, "Recordatorio $reminderId completado, agregando ${amount}ml")
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
            Log.w(TAG, "Error al obtener cantidad actual, usando 0.0")
            0.0f
        }
    }


    // Nueva función para guardar el estado completado en Firebase
    private fun saveReminderCompletedState(reminderId: Int) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "Usuario no autenticado para guardar estado de recordatorio")
            return
        }

        val uid = currentUser.uid
        val fecha = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(Date())

        val reminderData = mapOf(
            "completado" to true,
            "fechaCompletado" to System.currentTimeMillis(),
            "reminderId" to reminderId
        )

        Log.d(TAG, "Guardando estado completado para recordatorio $reminderId")

        db.collection("users").document(uid)
            .collection("hidratacion").document(fecha)
            .collection("recordatorios").document(reminderId.toString())
            .set(reminderData)
            .addOnSuccessListener {
                Log.d(TAG, "Estado de recordatorio $reminderId guardado exitosamente")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al guardar estado de recordatorio: ${exception.message}")
            }
    }

    // Nueva función para cargar recordatorios completados
    private fun loadCompletedReminders(uid: String, fecha: String) {
        Log.d(TAG, "Cargando recordatorios completados para fecha: $fecha")

        db.collection("users").document(uid)
            .collection("hidratacion").document(fecha)
            .collection("recordatorios")
            .get()
            .addOnSuccessListener { querySnapshot ->
                try {
                    val completedReminderIds = mutableSetOf<Int>()

                    for (document in querySnapshot.documents) {
                        val reminderId = document.id.toIntOrNull()
                        if (reminderId != null) {
                            completedReminderIds.add(reminderId)
                            Log.d(TAG, "Recordatorio $reminderId encontrado como completado")
                        }
                    }

                    Log.d(TAG, "Total recordatorios completados encontrados: ${completedReminderIds.size}")

                    // Cargar datos mock con los estados de completado
                    loadMockDataWithCompletedStates(completedReminderIds)
                    _isLoading.value = false

                } catch (e: Exception) {
                    Log.e(TAG, "Error al procesar recordatorios completados: ${e.message}")
                    loadMockData() // Fallback a datos mock normales
                    _isLoading.value = false
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al cargar recordatorios completados: ${exception.message}")
                loadMockData() // Fallback a datos mock normales
                _isLoading.value = false
            }
    }


    // Función modificada para cargar datos mock con estados de completado
    private fun loadMockDataWithCompletedStates(completedReminderIds: Set<Int>) {
        try {
            Log.d(TAG, "Cargando datos mock con estados completados: $completedReminderIds")

            // Meta diaria y tip
            _dailyGoal.value = "Meta diaria 2.5L"
            _dailyTip.value = "Mantente hidratado para despertar por dentro tu belleza natural. La hidratación facilita el flujo sanguíneo y aporta la lucidez en piel y cuerpo que tanto amas."

            // Crear recordatorios base
            val recordatoriosBase = listOf(
                RecordatorioHidratacion(
                    id = 1,
                    hora = "06:00 - 09:00",
                    descripcion = "Al despertar",
                    cantidad = "500ml",
                    completado = false,
                    tipo = TipoRecordatorio.MAÑANA
                ),
                RecordatorioHidratacion(
                    id = 2,
                    hora = "09:00 - 11:00",
                    descripcion = "Media mañana",
                    cantidad = "250ml",
                    completado = false,
                    tipo = TipoRecordatorio.MAÑANA
                ),
                RecordatorioHidratacion(
                    id = 3,
                    hora = "11:00 - 13:00",
                    descripcion = "Antes de almorzar",
                    cantidad = "500ml",
                    completado = false,
                    tipo = TipoRecordatorio.TARDE
                ),
                RecordatorioHidratacion(
                    id = 4,
                    hora = "13:00 - 15:00",
                    descripcion = "Después de almorzar",
                    cantidad = "300ml",
                    completado = false,
                    tipo = TipoRecordatorio.TARDE
                ),
                RecordatorioHidratacion(
                    id = 5,
                    hora = "15:00 - 18:00",
                    descripcion = "Tarde",
                    cantidad = "500ml",
                    completado = false,
                    tipo = TipoRecordatorio.TARDE
                ),
                RecordatorioHidratacion(
                    id = 6,
                    hora = "18:00 - 20:00",
                    descripcion = "Antes de cenar",
                    cantidad = "250ml",
                    completado = false,
                    tipo = TipoRecordatorio.NOCHE
                ),
                RecordatorioHidratacion(
                    id = 7,
                    hora = "20:00 - 22:00",
                    descripcion = "Noche",
                    cantidad = "200ml",
                    completado = false,
                    tipo = TipoRecordatorio.NOCHE
                )
            )

            // Actualizar estados de completado desde Firebase
            val recordatorios = recordatoriosBase.map { recordatorio ->
                val estaCompletado = completedReminderIds.contains(recordatorio.id)
                recordatorio.copy(completado = estaCompletado)
            }

            Log.d(TAG, "Recordatorios con estados aplicados:")
            recordatorios.forEach { recordatorio ->
                Log.d(TAG, "ID ${recordatorio.id}: ${if (recordatorio.completado) "Completado" else "Pendiente"}")
            }

            _hydrationReminders.value = recordatorios

            // Resto de datos mock...
            val stats = EstadisticasHidratacion(
                promedioSemanal = 2.1f,
                mejorDia = "Lunes - 2.8L",
                rachaActual = 5,
                totalSemana = 14.7f,
                porcentajeObjetivo = 72
            )
            _hydrationStats.value = stats

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

            Log.d(TAG, "Datos mock cargados exitosamente: ${recordatorios.size} recordatorios")

        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar datos mock con estados: ${e.message}", e)
            _errorMessage.value = "Error al procesar los datos"
        }
    }


    private fun extractAmountFromString(cantidadStr: String): Float {
        return try {
            cantidadStr.replace("ml", "").trim().toFloat()
        } catch (e: Exception) {
            Log.w(TAG, "Error al extraer cantidad de '$cantidadStr', usando 250ml por defecto")
            250f // valor por defecto
        }
    }

    private fun loadMockData() {
        try {
            Log.d(TAG, "Cargando datos mock...")

            // Meta diaria y tip
            _dailyGoal.value = "Meta diaria 2.5L"
            _dailyTip.value = "Mantente hidratado para despertar por dentro tu belleza natural. La hidratación facilita el flujo sanguíneo y aporta la lucidez en piel y cuerpo que tanto amas."

            // Recordatorios de hidratación con más variedad
            val recordatorios = listOf(
                RecordatorioHidratacion(
                    id = 1,
                    hora = "06:00 - 9:00",
                    descripcion = "Al despertar",
                    cantidad = "500ml",
                    completado = false,
                    tipo = TipoRecordatorio.MAÑANA
                ),
                RecordatorioHidratacion(
                    id = 2,
                    hora = "9:00 - 11:00",
                    descripcion = "Media mañana",
                    cantidad = "250ml",
                    completado = false,
                    tipo = TipoRecordatorio.MAÑANA
                ),
                RecordatorioHidratacion(
                    id = 3,
                    hora = "11:00 - 13:00",
                    descripcion = "Antes de almorzar",
                    cantidad = "500ml",
                    completado = false,
                    tipo = TipoRecordatorio.TARDE
                ),
                RecordatorioHidratacion(
                    id = 4,
                    hora = "13:00 - 15:00",
                    descripcion = "Después de almorzar",
                    cantidad = "300ml",
                    completado = false,
                    tipo = TipoRecordatorio.TARDE
                ),
                RecordatorioHidratacion(
                    id = 5,
                    hora = "15:00 - 18:00",
                    descripcion = "Tarde",
                    cantidad = "500ml",
                    completado = false,
                    tipo = TipoRecordatorio.TARDE
                ),
                RecordatorioHidratacion(
                    id = 6,
                    hora = "18:00 - 20:00",
                    descripcion = "Antes de cenar",
                    cantidad = "250ml",
                    completado = false,
                    tipo = TipoRecordatorio.NOCHE
                ),
                RecordatorioHidratacion(
                    id = 7,
                    hora = "20:00 - 22:00",
                    descripcion = "Noche",
                    cantidad = "200ml",
                    completado = false,
                    tipo = TipoRecordatorio.NOCHE
                )
            )
            _hydrationReminders.value = recordatorios

            // Estadísticas de hidratación
            val stats = EstadisticasHidratacion(
                promedioSemanal = 2.1f,
                mejorDia = "Lunes - 2.8L",
                rachaActual = 5,
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

            Log.d(TAG, "Datos mock cargados exitosamente: ${recordatorios.size} recordatorios")

        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar datos mock: ${e.message}", e)
            _errorMessage.value = "Error al procesar los datos"
        }
    }

    private fun loadMockDataOnly() {
        try {
            Log.d(TAG, "Cargando solo datos mock (sin Firebase)")

            // Valores por defecto sin Firebase
            _currentHydration.value = "0.0 L"
            _waterGlassLevel.value = 0
            _isLoading.value = true

            loadMockData()

            _isLoading.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar datos mock únicamente: ${e.message}", e)
            _isLoading.value = false
        }
    }

    // Función para verificar si un recordatorioo debe estar habilitado
    fun isReminderEnabled(reminder: RecordatorioHidratacion): Boolean {
        // Si ya está completado, no debe estar habilitado
        if (reminder.completado) {
            return false
        }

        // Verificar si está en el horario activo
        return isReminderInActiveTime(reminder)
    }

    // Función para verificar si un recordatorio está en su horario activo
    private fun isReminderInActiveTime(reminder: RecordatorioHidratacion): Boolean {
        try {
            val currentTime = Calendar.getInstance()
            val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
            val currentMinute = currentTime.get(Calendar.MINUTE)
            val currentTotalMinutes = currentHour * 60 + currentMinute

            // Parsear el rango de horas (ej: "06:00 - 9:00")
            val horaRange = reminder.hora.split(" - ")
            if (horaRange.size != 2) return true // Si no se puede parsear, habilitar siempre

            val startTime = parseTimeToMinutes(horaRange[0].trim())
            val endTime = parseTimeToMinutes(horaRange[1].trim())

            val isInRange = currentTotalMinutes >= startTime && currentTotalMinutes <= endTime

            Log.d(TAG, "Recordatorio ${reminder.id}: Hora actual ${currentHour}:${String.format("%02d", currentMinute)} (${currentTotalMinutes}min), Rango: ${startTime}-${endTime}min, En rango: $isInRange")

            return isInRange

        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar horario para recordatorio ${reminder.id}: ${e.message}")
            return true // En caso de error, habilitar el botón
        }
    }

    // Función auxiliar para convertir hora (HH:mm) a minutos totales
    private fun parseTimeToMinutes(timeStr: String): Int {
        try {
            val parts = timeStr.split(":")
            val hour = parts[0].toInt()
            val minute = if (parts.size > 1) parts[1].toInt() else 0
            return hour * 60 + minute
        } catch (e: Exception) {
            Log.e(TAG, "Error al parsear tiempo '$timeStr': ${e.message}")
            return 0
        }
    }

    // Funciones adicionales útiles
    fun refreshData() {
        loadHydrationData(null)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Función de debug para verificar la conexión
    fun debugFirebaseConnection() {
        val currentUser = auth.currentUser
        Log.d(TAG, "=== DEBUG FIREBASE CONNECTION ===")
        Log.d(TAG, "Usuario autenticado: ${currentUser != null}")
        Log.d(TAG, "UID: ${currentUser?.uid}")
        Log.d(TAG, "Email: ${currentUser?.email}")

        if (currentUser != null) {
            val fecha = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(Date())
            Log.d(TAG, "Fecha para documento: $fecha")
            Log.d(TAG, "Ruta completa: users/${currentUser.uid}/hidratacion/$fecha")

            // Intentar leer documento específico
            db.collection("users").document(currentUser.uid)
                .collection("hidratacion").document(fecha)
                .get()
                .addOnSuccessListener { document ->
                    Log.d(TAG, "DEBUG: Documento existe: ${document.exists()}")
                    Log.d(TAG, "DEBUG: Datos: ${document.data}")
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "DEBUG: Error al leer documento: ${exception.message}")
                }

            // También verificar si el documento del usuario existe
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { userDoc ->
                    Log.d(TAG, "DEBUG: Documento de usuario existe: ${userDoc.exists()}")
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "DEBUG: Error al leer usuario: ${exception.message}")
                }
        }
        Log.d(TAG, "=== FIN DEBUG ===")
    }

    fun resetDailyProgress() {
        try {
            Log.d(TAG, "Reseteando progreso diario")

            _currentHydration.value = "0.0 L"
            _waterGlassLevel.value = 0

            // Resetear recordatorios
            val resetReminders = _hydrationReminders.value?.map { it.copy(completado = false) }
            _hydrationReminders.value = resetReminders ?: emptyList()

            // Si hay usuario autenticado, también resetear en Firebase
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val uid = currentUser.uid
                val fecha = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(Date())

                Log.d(TAG, "Reseteando datos en Firebase - UID: $uid, Fecha: $fecha")

                // Usar "users" como en PerfilViewModel
                db.collection("users").document(uid).collection("hidratacion").document(fecha)
                    .delete()
                    .addOnSuccessListener {
                        Log.d(TAG, "Datos de Firebase reseteados")
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Error al resetear Firebase: ${exception.message}")
                    }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al resetear progreso: ${e.message}", e)
        }
    }

    // Data classes para estructurar los datos
    data class RecordatorioHidratacion(
        val id: Int,
        val hora: String,

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