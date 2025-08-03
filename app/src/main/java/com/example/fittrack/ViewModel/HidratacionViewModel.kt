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
        Log.d(TAG, "=== INICIANDO CARGA DE DATOS DE HIDRATACIÓN ===")
        Log.d(TAG, "Usuario: $userName")

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

        Log.d(TAG, "Cargando para UID: $uid, Fecha: $fecha")

        // PASO 1: Cargar datos principales de hidratación
        loadMainHydrationData(uid, fecha)
    }

    private fun loadMainHydrationData(uid: String, fecha: String) {
        Log.d(TAG, "=== PASO 1: Cargando datos principales ===")

        db.collection("users").document(uid).collection("hidratacion").document(fecha)
            .get()
            .addOnSuccessListener { document ->
                try {
                    Log.d(TAG, "Datos principales obtenidos. Documento existe: ${document.exists()}")

                    if (document.exists()) {
                        val litros = document.getDouble("litros") ?: 0.0

                        // Actualizar UI con hidratación
                        _currentHydration.value = String.format("%.1f L", litros)
                        val porcentaje = ((litros / 2.5) * 100).toInt().coerceAtMost(100)
                        _waterGlassLevel.value = porcentaje

                        Log.d(TAG, "✅ Datos principales cargados: ${litros}L (${porcentaje}%)")

                        // ✅ BUSCAR RECORDATORIOS COMPLETADOS EN AMBOS LUGARES
                        loadCompletedRemindersFromBothSources(uid, fecha, document)

                    } else {
                        Log.d(TAG, "No hay datos principales, iniciando en 0")
                        _currentHydration.value = "0.0 L"
                        _waterGlassLevel.value = 0

                        // Buscar solo en subcolección
                        loadCompletedReminders(uid, fecha)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error al procesar datos principales: ${e.message}", e)
                    _errorMessage.value = "Error al procesar datos: ${e.message}"
                    loadCompletedReminders(uid, fecha)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al cargar datos principales: ${exception.message}", exception)
                _errorMessage.value = "Error de conexión: ${exception.message}"
                loadCompletedReminders(uid, fecha)
            }
    }


    private fun loadCompletedRemindersFromBothSources(uid: String, fecha: String, mainDocument: com.google.firebase.firestore.DocumentSnapshot) {
        Log.d(TAG, "=== BUSCANDO RECORDATORIOS EN AMBAS FUENTES ===")

        try {
            // Primero, obtener recordatorios del documento principal
            val completedFromMain = if (mainDocument.contains("recordatoriosCompletados")) {
                val completedData = mainDocument.get("recordatoriosCompletados") as? List<*> ?: emptyList<Any>()
                completedData.mapNotNull { item ->
                    when (item) {
                        is Long -> item.toInt()
                        is Int -> item
                        is String -> item.toIntOrNull()
                        else -> null
                    }
                }.toSet()
            } else {
                emptySet<Int>()
            }

            Log.d(TAG, "Recordatorios desde documento principal: $completedFromMain")

            // Luego, buscar en subcolección para tener datos completos
            db.collection("users").document(uid)
                .collection("hidratacion").document(fecha)
                .collection("recordatorios")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    try {
                        val completedFromSubcollection = mutableSetOf<Int>()

                        Log.d(TAG, "Subcolección obtenida. Documentos: ${querySnapshot.size()}")

                        for (document in querySnapshot.documents) {
                            val reminderId = document.id.toIntOrNull()
                            val completado = document.getBoolean("completado") ?: false

                            Log.d(TAG, "Subcolección - Doc: ${document.id}, reminderId: $reminderId, completado: $completado")

                            if (reminderId != null && completado) {
                                completedFromSubcollection.add(reminderId)
                            }
                        }

                        // ✅ COMBINAR AMBAS FUENTES (UNIÓN)
                        val allCompletedReminders = completedFromMain + completedFromSubcollection

                        Log.d(TAG, "=== RESUMEN COMPLETO DE RECORDATORIOS ===")
                        Log.d(TAG, "Desde documento principal: $completedFromMain")
                        Log.d(TAG, "Desde subcolección: $completedFromSubcollection")
                        Log.d(TAG, "TOTAL COMBINADO: $allCompletedReminders")

                        // Aplicar estados
                        loadMockDataWithCompletedStates(allCompletedReminders)

                    } catch (e: Exception) {
                        Log.e(TAG, "Error al procesar subcolección: ${e.message}", e)
                        // Usar solo datos del documento principal
                        loadMockDataWithCompletedStates(completedFromMain)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error al cargar subcolección: ${exception.message}", exception)
                    // Usar solo datos del documento principal
                    loadMockDataWithCompletedStates(completedFromMain)
                }

        } catch (e: Exception) {
            Log.e(TAG, "Error en loadCompletedRemindersFromBothSources: ${e.message}", e)
            loadBasicMockData()
        }
    }

    private fun loadCompletedReminders(uid: String, fecha: String) {
        Log.d(TAG, "=== PASO 2: Cargando recordatorios completados ===")
        Log.d(TAG, "Ruta: users/$uid/hidratacion/$fecha/recordatorios")

        db.collection("users").document(uid)
            .collection("hidratacion").document(fecha)
            .collection("recordatorios")
            .get()
            .addOnSuccessListener { querySnapshot ->
                try {
                    val completedReminderIds = mutableSetOf<Int>()

                    Log.d(TAG, "QuerySnapshot obtenido. Documentos: ${querySnapshot.size()}")

                    for (document in querySnapshot.documents) {
                        val reminderId = document.id.toIntOrNull()
                        val completado = document.getBoolean("completado") ?: false

                        Log.d(TAG, "Documento: ${document.id}, reminderId: $reminderId, completado: $completado")

                        if (reminderId != null && completado) {
                            completedReminderIds.add(reminderId)
                            Log.d(TAG, "✓ Recordatorio $reminderId marcado como completado")
                        }
                    }

                    Log.d(TAG, "=== RESUMEN RECORDATORIOS COMPLETADOS ===")
                    Log.d(TAG, "Total encontrados: ${completedReminderIds.size}")
                    Log.d(TAG, "IDs completados: $completedReminderIds")

                    // PASO 3: Cargar datos mock con estados aplicados
                    loadMockDataWithCompletedStates(completedReminderIds)

                } catch (e: Exception) {
                    Log.e(TAG, "Error al procesar recordatorios: ${e.message}", e)
                    _errorMessage.value = "Error al procesar recordatorios: ${e.message}"
                    // Fallback: cargar datos mock sin estados
                    loadBasicMockData()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al cargar recordatorios: ${exception.message}", exception)
                // Fallback: cargar datos mock sin estados
                loadBasicMockData()
            }
    }

    private fun loadMockDataWithCompletedStates(completedReminderIds: Set<Int>) {
        try {
            Log.d(TAG, "=== PASO 3: Aplicando estados completados ===")
            Log.d(TAG, "Estados a aplicar: $completedReminderIds")

            // Meta diaria y tip
            _dailyGoal.value = "Meta diaria 2.5L"
            _dailyTip.value = "Mantente hidratado para despertar por dentro tu belleza natural. La hidratación facilita el flujo sanguíneo y aporta la lucidez en piel y cuerpo que tanto amas."

            // Crear recordatorios base
            val recordatoriosBase = getBaseReminders()

            // ✅ APLICAR ESTADOS DE COMPLETADO CON LOGGING DETALLADO
            val recordatorios = recordatoriosBase.map { recordatorio ->
                val estaCompletado = completedReminderIds.contains(recordatorio.id)
                val recordatorioFinal = recordatorio.copy(completado = estaCompletado)

                Log.d(TAG, "📋 Recordatorio ${recordatorio.id} (${recordatorio.descripcion}): ${if (estaCompletado) "✅ COMPLETADO" else "⏳ PENDIENTE"}")

                recordatorioFinal
            }

            _hydrationReminders.value = recordatorios

            // Cargar resto de datos mock
            loadAdditionalMockData()

            _isLoading.value = false

            Log.d(TAG, "=== CARGA COMPLETADA EXITOSAMENTE ===")
            Log.d(TAG, "Total recordatorios: ${recordatorios.size}")
            Log.d(TAG, "Recordatorios completados: ${recordatorios.count { it.completado }}")
            Log.d(TAG, "IDs completados aplicados: ${recordatorios.filter { it.completado }.map { it.id }}")

        } catch (e: Exception) {
            Log.e(TAG, "Error al aplicar estados completados: ${e.message}", e)
            _errorMessage.value = "Error al procesar los estados"
            loadBasicMockData()
        }
    }

    private fun loadBasicMockData() {
        try {
            Log.d(TAG, "=== CARGANDO DATOS MOCK BÁSICOS (SIN ESTADOS) ===")

            _dailyGoal.value = "Meta diaria 2.5L"
            _dailyTip.value = "Mantente hidratado para despertar por dentro tu belleza natural. La hidratación facilita el flujo sanguíneo y aporta la lucidez en piel y cuerpo que tanto amas."

            val recordatorios = getBaseReminders()
            _hydrationReminders.value = recordatorios

            loadAdditionalMockData()
            _isLoading.value = false

            Log.d(TAG, "Datos mock básicos cargados: ${recordatorios.size} recordatorios")

        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar datos mock básicos: ${e.message}", e)
            _isLoading.value = false
        }
    }

    private fun getBaseReminders(): List<RecordatorioHidratacion> {
        return listOf(
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
    }

    private fun loadAdditionalMockData() {
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
    }

    fun completeReminder(reminderId: Int) {
        try {
            Log.d(TAG, "=== COMPLETANDO RECORDATORIO $reminderId ===")

            val currentReminders = _hydrationReminders.value?.toMutableList() ?: return
            val reminderIndex = currentReminders.indexOfFirst { it.id == reminderId }

            if (reminderIndex == -1) {
                Log.e(TAG, "Recordatorio $reminderId no encontrado")
                return
            }

            val reminder = currentReminders[reminderIndex]

            if (reminder.completado) {
                Log.w(TAG, "Recordatorio $reminderId ya está completado")
                return
            }

            // 1. Actualizar estado local inmediatamente
            val updatedReminder = reminder.copy(completado = true)
            currentReminders[reminderIndex] = updatedReminder
            _hydrationReminders.value = currentReminders

            // 2. Calcular nueva cantidad total
            val amount = extractAmountFromString(updatedReminder.cantidad)
            val currentAmount = getCurrentHydrationAmount()
            val newTotalAmount = currentAmount + (amount / 1000f)

            // ✅ REDONDEAR PARA LA UI
            val roundedAmount = String.format("%.1f", newTotalAmount).toFloat()

            // 3. Actualizar UI inmediatamente con valor redondeado
            _currentHydration.value = String.format("%.1f L", roundedAmount)
            val percentage = ((roundedAmount / 2.5f) * 100).toInt().coerceAtMost(100)
            _waterGlassLevel.value = percentage

            // 4. Guardar TODO en Firebase con valor redondeado
            saveCompleteHydrationData(reminderId, roundedAmount)

            Log.d(TAG, "Recordatorio $reminderId completado: +${amount}ml, Total: ${roundedAmount}L")

        } catch (e: Exception) {
            Log.e(TAG, "Error al completar recordatorio $reminderId: ${e.message}", e)
            _errorMessage.value = "Error al completar recordatorio"
        }
    }


    // ✅ NUEVO MÉTODO UNIFICADO PARA GUARDAR DATOS
    private fun saveCompleteHydrationData(reminderId: Int, totalLiters: Float) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "Usuario no autenticado")
            return
        }

        val uid = currentUser.uid
        val fecha = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(Date())
        val roundedLiters = String.format("%.1f", totalLiters).toDouble()

        Log.d(TAG, "=== GUARDANDO DATOS COMPLETOS DE HIDRATACIÓN ===")
        Log.d(TAG, "UID: $uid, Fecha: $fecha, Recordatorio: $reminderId")
        Log.d(TAG, "Total redondeado: ${roundedLiters}L")

        // ✅ TRABAJAR SOLO CON LA SUBCOLECCIÓN hidratacion
        db.collection("users").document(uid)
            .collection("hidratacion").document(fecha)
            .get()
            .addOnSuccessListener { document ->
                try {
                    // Obtener lista actual de recordatorios completados
                    val currentCompletedReminders = if (document.exists()) {
                        val completedData = document.get("recordatoriosCompletados") as? List<*> ?: emptyList<Any>()
                        completedData.mapNotNull { item ->
                            when (item) {
                                is Long -> item.toInt()
                                is Int -> item
                                is String -> item.toIntOrNull()
                                else -> null
                            }
                        }.toMutableList()
                    } else {
                        mutableListOf<Int>()
                    }

                    // Agregar nuevo recordatorio si no existe
                    if (!currentCompletedReminders.contains(reminderId)) {
                        currentCompletedReminders.add(reminderId)
                    }

                    // ✅ DATOS COMPLETOS PARA LA SUBCOLECCIÓN (SIN tocar documento padre)
                    val completeData = mapOf(
                        "litros" to roundedLiters,
                        "fecha" to fecha,
                        "recordatoriosCompletados" to currentCompletedReminders,
                        "lastUpdated" to System.currentTimeMillis(),
                        "uid" to uid
                    )

                    Log.d(TAG, "Guardando en subcolección hidratacion: $completeData")

                    // ✅ GUARDAR SOLO EN SUBCOLECCIÓN hidratacion
                    db.collection("users").document(uid)
                        .collection("hidratacion").document(fecha)
                        .set(completeData)
                        .addOnSuccessListener {
                            Log.d(TAG, "✅ Datos de hidratación guardados exitosamente")

                            // Guardar recordatorio individual también
                            saveIndividualReminder(uid, fecha, reminderId)

                            // Notificar cambio
                            notifyHydrationChanged()
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "❌ Error al guardar datos de hidratación: ${exception.message}", exception)
                            _errorMessage.value = "Error al guardar: ${exception.message}"
                        }

                } catch (e: Exception) {
                    Log.e(TAG, "Error al procesar datos para guardar: ${e.message}", e)
                    _errorMessage.value = "Error al procesar datos: ${e.message}"
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al obtener datos actuales: ${exception.message}", exception)
                _errorMessage.value = "Error de conexión: ${exception.message}"
            }
    }

    private fun saveIndividualReminder(uid: String, fecha: String, reminderId: Int) {
        val reminderData = mapOf(
            "completado" to true,
            "fechaCompletado" to System.currentTimeMillis(),
            "reminderId" to reminderId
        )

        db.collection("users").document(uid)
            .collection("hidratacion").document(fecha)
            .collection("recordatorios").document(reminderId.toString())
            .set(reminderData)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Recordatorio individual $reminderId guardado")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "⚠️ Error al guardar recordatorio individual: ${exception.message}")
                // No es crítico, el dato principal ya se guardó
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

    private fun extractAmountFromString(cantidadStr: String): Float {
        return try {
            cantidadStr.replace("ml", "").trim().toFloat()
        } catch (e: Exception) {
            Log.w(TAG, "Error al extraer cantidad de '$cantidadStr', usando 250ml por defecto")
            250f
        }
    }

    private fun loadMockDataOnly() {
        try {
            Log.d(TAG, "=== CARGANDO SOLO DATOS MOCK (SIN FIREBASE) ===")

            _currentHydration.value = "0.0 L"
            _waterGlassLevel.value = 0
            _isLoading.value = true

            loadBasicMockData()

        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar datos mock únicamente: ${e.message}", e)
            _isLoading.value = false
        }
    }

    // Función para verificar si un recordatorio debe estar habilitado
    fun isReminderEnabled(reminder: RecordatorioHidratacion): Boolean {
        // Si ya está completado, no debe estar habilitado
        if (reminder.completado) {
            return false
        }

        // Verificar si está en el horario activo
        return isReminderInActiveTime(reminder)
    }

    private fun isReminderInActiveTime(reminder: RecordatorioHidratacion): Boolean {
        try {
            val currentTime = Calendar.getInstance()
            val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
            val currentMinute = currentTime.get(Calendar.MINUTE)
            val currentTotalMinutes = currentHour * 60 + currentMinute

            val horaRange = reminder.hora.split(" - ")
            if (horaRange.size != 2) return true

            val startTime = parseTimeToMinutes(horaRange[0].trim())
            val endTime = parseTimeToMinutes(horaRange[1].trim())

            val isInRange = currentTotalMinutes >= startTime && currentTotalMinutes <= endTime

            Log.d(TAG, "Recordatorio ${reminder.id}: Hora actual ${currentHour}:${String.format("%02d", currentMinute)} (${currentTotalMinutes}min), Rango: ${startTime}-${endTime}min, En rango: $isInRange")

            return isInRange

        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar horario para recordatorio ${reminder.id}: ${e.message}")
            return true
        }
    }

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

    fun refreshData() {
        Log.d(TAG, "=== REFRESCANDO DATOS ===")
        loadHydrationData(null)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun debugCompletedReminders() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "DEBUG: No hay usuario autenticado")
            return
        }

        val uid = currentUser.uid
        val fecha = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(Date())

        Log.d(TAG, "=== DEBUG COMPLETO DE RECORDATORIOS ===")
        Log.d(TAG, "UID: $uid")
        Log.d(TAG, "Fecha: $fecha")

        // Debug documento principal
        db.collection("users").document(uid)
            .collection("hidratacion").document(fecha)
            .get()
            .addOnSuccessListener { document ->
                Log.d(TAG, "📄 DOCUMENTO PRINCIPAL:")
                Log.d(TAG, "   Existe: ${document.exists()}")
                if (document.exists()) {
                    Log.d(TAG, "   Litros: ${document.getDouble("litros")}")
                    Log.d(TAG, "   Recordatorios completados: ${document.get("recordatoriosCompletados")}")
                    Log.d(TAG, "   Datos completos: ${document.data}")
                }

                // Debug subcolección
                db.collection("users").document(uid)
                    .collection("hidratacion").document(fecha)
                    .collection("recordatorios")
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        Log.d(TAG, "📁 SUBCOLECCIÓN RECORDATORIOS:")
                        Log.d(TAG, "   Total documentos: ${querySnapshot.size()}")

                        for (doc in querySnapshot.documents) {
                            Log.d(TAG, "   📝 Doc ${doc.id}: ${doc.data}")
                        }

                        Log.d(TAG, "=== FIN DEBUG ===")
                    }
            }
    }

    fun forceReloadReminders() {
        Log.d(TAG, "=== FORZANDO RECARGA DE RECORDATORIOS ===")
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val fecha = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(Date())
            loadMainHydrationData(currentUser.uid, fecha)
        }
    }


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

    fun diagnosticFirestorePermissions() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "DIAGNOSTIC: No hay usuario autenticado")
            return
        }

        val uid = currentUser.uid
        val fecha = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(Date())

        Log.d(TAG, "=== DIAGNÓSTICO DE PERMISOS FIRESTORE ===")
        Log.d(TAG, "UID: $uid")
        Log.d(TAG, "Fecha: $fecha")
        Log.d(TAG, "Auth UID: ${currentUser.uid}")
        Log.d(TAG, "Email: ${currentUser.email}")

        // ✅ TEST 1: NO TOCAR el documento principal del usuario, solo leerlo
        Log.d(TAG, "TEST 1: Leyendo documento principal del usuario...")
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { userDoc ->
                Log.d(TAG, "✓ TEST 1: Lectura de documento principal EXITOSA")
                Log.d(TAG, "   Documento existe: ${userDoc.exists()}")
                if (userDoc.exists()) {
                    Log.d(TAG, "   Campos: ${userDoc.data?.keys}")
                }

                // ✅ TEST 2: Escribir directamente en subcolección hidratacion (SIN tocar documento padre)
                Log.d(TAG, "TEST 2: Escribiendo en subcolección hidratacion...")
                db.collection("users").document(uid)
                    .collection("hidratacion").document(fecha)
                    .set(mapOf(
                        "litros" to 0.0,
                        "fecha" to fecha,
                        "recordatoriosCompletados" to emptyList<Int>(),
                        "lastUpdated" to System.currentTimeMillis(),
                        "uid" to uid
                    ))
                    .addOnSuccessListener {
                        Log.d(TAG, "✓ TEST 2: Escritura en hidratacion EXITOSA")

                        // ✅ TEST 3: Escribir en subcolección recordatorios
                        Log.d(TAG, "TEST 3: Escribiendo en subcolección recordatorios...")
                        db.collection("users").document(uid)
                            .collection("hidratacion").document(fecha)
                            .collection("recordatorios").document("test")
                            .set(mapOf(
                                "completado" to false,
                                "reminderId" to 999,
                                "timestamp" to System.currentTimeMillis()
                            ))
                            .addOnSuccessListener {
                                Log.d(TAG, "✓ TEST 3: Escritura en recordatorios EXITOSA")
                                Log.d(TAG, "=== TODOS LOS TESTS PASARON ===")
                            }
                            .addOnFailureListener { exception ->
                                Log.e(TAG, "✗ TEST 3: Error en recordatorios: ${exception.message}", exception)
                            }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "✗ TEST 2: Error en hidratacion: ${exception.message}", exception)
                    }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "✗ TEST 1: Error al leer documento principal: ${exception.message}", exception)
            }

        // ✅ Información adicional sin modificar datos
        Log.d(TAG, "=== INFORMACIÓN DE AUTENTICACIÓN ===")
        Log.d(TAG, "Usuario autenticado: ${auth.currentUser != null}")
        Log.d(TAG, "UID válido: ${auth.currentUser?.uid}")
    }

    fun resetDailyProgress() {
        try {
            Log.d(TAG, "=== RESETEANDO PROGRESO DIARIO ===")

            _currentHydration.value = "0.0 L"
            _waterGlassLevel.value = 0

            val resetReminders = _hydrationReminders.value?.map { it.copy(completado = false) }
            _hydrationReminders.value = resetReminders ?: emptyList()

            val currentUser = auth.currentUser
            if (currentUser != null) {
                val uid = currentUser.uid
                val fecha = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(Date())

                Log.d(TAG, "Reseteando SOLO datos de hidratación - UID: $uid, Fecha: $fecha")

                // ✅ ELIMINAR SOLO EL DOCUMENTO DE HIDRATACIÓN, NO EL USUARIO
                db.collection("users").document(uid)
                    .collection("hidratacion").document(fecha)
                    .delete()
                    .addOnSuccessListener {
                        Log.d(TAG, "✓ Datos de hidratación reseteados (documento padre intacto)")
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "✗ Error al resetear hidratación: ${exception.message}")
                    }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al resetear progreso: ${e.message}", e)
        }
    }

    // Data classes
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

    private var onHydrationChangedListener: ((Double) -> Unit)? = null

    fun setOnHydrationChangedListener(listener: (Double) -> Unit) {
        onHydrationChangedListener = listener
    }

    private fun notifyHydrationChanged() {
        try {
            val currentAmount = getCurrentHydrationAmount()
            Log.d(TAG, "📢 Notificando cambio de hidratación: ${currentAmount}L")
            onHydrationChangedListener?.invoke(currentAmount.toDouble())
        } catch (e: Exception) {
            Log.e(TAG, "Error al notificar cambio de hidratación: ${e.message}", e)
        }
    }

    // ✅ MODIFICAR LA FUNCIÓN updateLocalHydration PARA INCLUIR LA NOTIFICACIÓN
    private fun updateLocalHydrationWithNotification(amount: Float) {
        try {
            val currentAmount = getCurrentHydrationAmount()
            val newAmount = currentAmount + amount

            _currentHydration.value = String.format("%.1f L", newAmount)
            val percentage = ((newAmount / 2.5f) * 100).toInt().coerceAtMost(100)
            _waterGlassLevel.value = percentage

            Log.d(TAG, "UI actualizada: ${newAmount}L, ${percentage}%")

            // ✅ NOTIFICAR EL CAMBIO
            notifyHydrationChanged()

        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar UI local: ${e.message}", e)
        }
    }



}