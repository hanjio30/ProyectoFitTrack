package com.example.fittrack.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fittrack.network.RecorridoRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel : ViewModel() {

    // Firebase
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().getReference()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val recorridoRepository = RecorridoRepository() // ✅ AGREGAR REPOSITORY

    // LiveData para la UI
    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _greeting = MutableLiveData<String>()
    val greeting: LiveData<String> = _greeting

    private val _subtitle = MutableLiveData<String>()
    val subtitle: LiveData<String> = _subtitle

    private val _caloriesValue = MutableLiveData<String>()
    val caloriesValue: LiveData<String> = _caloriesValue

    private val _activityValue = MutableLiveData<String>()
    val activityValue: LiveData<String> = _activityValue

    private val _stepsValue = MutableLiveData<String>()
    val stepsValue: LiveData<String> = _stepsValue

    private val _hydrationValue = MutableLiveData<String>()
    val hydrationValue: LiveData<String> = _hydrationValue

    private val _streakValue = MutableLiveData<String>()
    val streakValue: LiveData<String> = _streakValue

    private val _distanceValue = MutableLiveData<String>()
    val distanceValue: LiveData<String> = _distanceValue

    private val _goalValue = MutableLiveData<String>()
    val goalValue: LiveData<String> = _goalValue

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    companion object {
        private const val TAG = "DashboardViewModel"
    }

    // Data classes para los modelos
    data class UserStats(
        val calories: Int = 0,
        val activityMinutes: Int = 0,
        val steps: Int = 0,
        val hydrationLiters: Double = 0.0,
        val streak: Int = 0,
        val distanceKm: Double = 0.0,
        val goalProgress: Int = 0
    )

    data class UserProfile(
        val fullName: String = "",
        val email: String = "",
        val uid: String = ""
    )

    // ✅ NUEVA DATA CLASS PARA ESTADÍSTICAS DE RECORRIDOS
    data class RecorridoStats(
        val totalCalorias: Int = 0,
        val totalMinutos: Int = 0,
        val totalPasos: Int = 0,
        val totalDistanciaKm: Double = 0.0
    )

    fun initializeDashboard(userName: String? = null) {
        try {
            Log.d(TAG, "=== INICIALIZANDO DashboardViewModel ===")

            _isLoading.value = true

            // ✅ ESTABLECER VALORES INICIALES LIMPIOS
            _hydrationValue.value = "0.0 lit."
            _caloriesValue.value = "0 kcal"
            _activityValue.value = "0 min"
            _stepsValue.value = "0"
            _streakValue.value = "0 días"
            _distanceValue.value = "0.0 km"
            _goalValue.value = "0%"

            if (!userName.isNullOrEmpty()) {
                setupUserData(userName)
            } else {
                loadUserData()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error en initializeDashboard: ${e.message}", e)
            handleError("Error al inicializar dashboard")
        }
    }

    private fun loadUserData() {
        try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                Log.d(TAG, "Cargando datos del usuario desde Firebase")

                databaseReference.child("users").child(currentUser.uid)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            try {
                                if (snapshot.exists()) {
                                    val userProfile = snapshot.getValue(UserProfile::class.java)
                                    setupUserData(userProfile?.fullName ?: "Usuario")
                                } else {
                                    setupUserData(currentUser.displayName ?: "Usuario")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al procesar datos del usuario: ${e.message}", e)
                                setupUserData("Usuario")
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(TAG, "Error al cargar datos del usuario: ${error.message}")
                            setupUserData("Usuario")
                        }
                    })
            } else {
                Log.w(TAG, "No hay usuario autenticado")
                setupUserData("Usuario")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en loadUserData: ${e.message}", e)
            setupUserData("Usuario")
        }
    }

    private fun setupUserData(name: String) {
        try {
            _userName.value = name
            setupGreeting(name)
            loadUserStats()
        } catch (e: Exception) {
            Log.e(TAG, "Error en setupUserData: ${e.message}", e)
            handleError("Error al configurar datos del usuario")
        }
    }

    private fun setupGreeting(name: String) {
        try {
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greetingPrefix = when (currentHour) {
                in 5..11 -> "¡BUENOS DÍAS"
                in 12..17 -> "¡BUENAS TARDES"
                else -> "¡BUENAS NOCHES"
            }

            _greeting.value = "$greetingPrefix ${name.uppercase()}!"

            val motivationalMessages = listOf(
                "Sigue avanzando hacia tu mejor versión.",
                "Cada día es una nueva oportunidad.",
                "Tu progreso de hoy cuenta para mañana.",
                "¡Mantén el impulso y alcanza tus metas!",
                "El éxito se construye día a día."
            )

            _subtitle.value = motivationalMessages.random()

        } catch (e: Exception) {
            Log.e(TAG, "Error en setupGreeting: ${e.message}", e)
            _greeting.value = "¡BIENVENIDO ${name.uppercase()}!"
            _subtitle.value = "Sigue avanzando hacia tu mejor versión."
        }
    }

    private fun loadUserStats() {
        viewModelScope.launch {
            try {
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    Log.d(TAG, "Cargando estadísticas del usuario")

                    // ✅ CARGAR ESTADÍSTICAS DE RECORRIDOS PRIMERO
                    loadRecorridosStats(currentUser.uid) { recorridoStats ->

                        // Luego cargar hidratación desde Firestore
                        loadHydrationFromFirestore(currentUser.uid) { hydrationLiters ->

                            // Finalmente cargar el resto de estadísticas desde Realtime Database
                            databaseReference.child("userStats").child(currentUser.uid)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        try {
                                            if (snapshot.exists()) {
                                                val userStats = snapshot.getValue(UserStats::class.java) ?: UserStats()

                                                // ✅ COMBINAR DATOS DE RECORRIDOS CON STATS EXISTENTES
                                                val updatedStats = userStats.copy(
                                                    hydrationLiters = hydrationLiters,
                                                    calories = recorridoStats.totalCalorias,
                                                    activityMinutes = recorridoStats.totalMinutos,
                                                    steps = recorridoStats.totalPasos,
                                                    distanceKm = recorridoStats.totalDistanciaKm
                                                )
                                                setupStatsData(updatedStats)
                                            } else {
                                                Log.d(TAG, "No hay estadísticas, usando datos de recorridos")
                                                setupStatsFromRecorridos(recorridoStats, hydrationLiters)
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error al procesar estadísticas: ${e.message}", e)
                                            setupStatsFromRecorridos(recorridoStats, hydrationLiters)
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Log.e(TAG, "Error al cargar estadísticas: ${error.message}")
                                        setupStatsFromRecorridos(recorridoStats, hydrationLiters)
                                    }
                                })
                        }
                    }
                } else {
                    setupDefaultStats()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en loadUserStats: ${e.message}", e)
                setupDefaultStats()
            }
        }
    }

    // ✅ FUNCIÓN MEJORADA PARA CARGAR ESTADÍSTICAS DE RECORRIDOS DE HOY
    private fun loadRecorridosStats(uid: String, callback: (RecorridoStats) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "=== CARGANDO ESTADÍSTICAS DE RECORRIDOS DE HOY ===")

                // Usar la nueva función para obtener estadísticas diarias
                val result = recorridoRepository.obtenerEstadisticasDiarias(uid)

                result.onSuccess { estadisticas ->
                    Log.d(TAG, "Estadísticas diarias obtenidas: $estadisticas")

                    val stats = RecorridoStats(
                        totalCalorias = estadisticas.totalCalorias,
                        totalMinutos = estadisticas.totalMinutos,
                        totalPasos = estadisticas.totalPasos,
                        totalDistanciaKm = estadisticas.totalDistanciaKm
                    )

                    Log.d(TAG, "✓ Estadísticas convertidas: $stats")
                    callback(stats)
                }

                result.onFailure { exception ->
                    Log.e(TAG, "Error al obtener estadísticas diarias: ${exception.message}", exception)
                    callback(RecorridoStats()) // Stats vacías
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error en loadRecorridosStats: ${e.message}", e)
                callback(RecorridoStats()) // Stats vacías
            }
        }
    }

    // ✅ NUEVA FUNCIÓN PARA CONFIGURAR STATS DESDE RECORRIDOS
    private fun setupStatsFromRecorridos(recorridoStats: RecorridoStats, hydrationLiters: Double) {
        try {
            _caloriesValue.value = "${recorridoStats.totalCalorias} kcal"
            _activityValue.value = "${recorridoStats.totalMinutos} min"
            _stepsValue.value = recorridoStats.totalPasos.toString()

            // Formatear hidratación a 1 decimal
            val formattedHydration = String.format("%.1f", hydrationLiters)
            _hydrationValue.value = "$formattedHydration lit."

            _streakValue.value = "20 días" // Mantener valor por defecto por ahora
            _distanceValue.value = String.format("%.1f km", recorridoStats.totalDistanciaKm)
            _goalValue.value = "75%" // Mantener valor por defecto por ahora

            _isLoading.value = false
            Log.d(TAG, "Estadísticas de recorridos configuradas")
        } catch (e: Exception) {
            Log.e(TAG, "Error en setupStatsFromRecorridos: ${e.message}", e)
            setupDefaultStats()
        }
    }

    private fun loadHydrationFromFirestore(uid: String, callback: (Double) -> Unit) {
        try {
            val fecha = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(Date())
            Log.d(TAG, "=== CARGANDO HIDRATACIÓN DESDE FIRESTORE ===")
            Log.d(TAG, "UID: $uid, Fecha: $fecha")

            firestore.collection("users").document(uid)
                .collection("hidratacion").document(fecha)
                .get()
                .addOnSuccessListener { document ->
                    try {
                        if (document.exists()) {
                            val litrosRaw = document.getDouble("litros") ?: 0.0
                            val litrosFormatted = String.format("%.1f", litrosRaw).toDouble()

                            Log.d(TAG, "✓ Hidratación encontrada - Raw: $litrosRaw, Formatted: $litrosFormatted")
                            callback(litrosFormatted)
                        } else {
                            Log.d(TAG, "No hay datos de hidratación para hoy, usando 0.0L")
                            callback(0.0)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al procesar datos de hidratación: ${e.message}", e)
                        callback(0.0)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "✗ Error al cargar hidratación desde Firestore: ${exception.message}", exception)
                    callback(0.0)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error en loadHydrationFromFirestore: ${e.message}", e)
            callback(0.0)
        }
    }

    private fun setupStatsData(userStats: UserStats) {
        try {
            _caloriesValue.value = "${userStats.calories} kcal"
            _activityValue.value = "${userStats.activityMinutes} min"
            _stepsValue.value = userStats.steps.toString()

            val formattedHydration = String.format("%.1f", userStats.hydrationLiters)
            _hydrationValue.value = "$formattedHydration lit."

            _streakValue.value = "${userStats.streak} días"
            _distanceValue.value = String.format("%.1f km", userStats.distanceKm)
            _goalValue.value = "${userStats.goalProgress}%"

            _isLoading.value = false
            Log.d(TAG, "Estadísticas configuradas - Hidratación: ${formattedHydration}L")
        } catch (e: Exception) {
            Log.e(TAG, "Error en setupStatsData: ${e.message}", e)
            setupDefaultStats()
        }
    }

    private fun setupDefaultStats(realHydration: Double = 0.0) {
        try {
            _caloriesValue.value = "0 kcal"
            _activityValue.value = "0 min"
            _stepsValue.value = "0"

            val formattedHydration = String.format("%.1f", realHydration)
            _hydrationValue.value = "$formattedHydration lit."

            _streakValue.value = "0 días"
            _distanceValue.value = "0.0 km"
            _goalValue.value = "0%"

            _isLoading.value = false
            Log.d(TAG, "Datos por defecto configurados con hidratación real: ${formattedHydration}L")
        } catch (e: Exception) {
            Log.e(TAG, "Error en setupDefaultStats: ${e.message}", e)
            handleError("Error al configurar estadísticas")
        }
    }

    fun refreshData() {
        try {
            Log.d(TAG, "Refrescando datos del dashboard")
            initializeDashboard()
        } catch (e: Exception) {
            Log.e(TAG, "Error al refrescar datos: ${e.message}", e)
            handleError("Error al refrescar datos")
        }
    }

    fun updateHydrationValue(liters: Double) {
        try {
            val formattedLiters = String.format("%.1f", liters)
            _hydrationValue.value = "$formattedLiters lit."

            Log.d(TAG, "Actualizando valor de hidratación en dashboard: ${formattedLiters}L")
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar hidratación: ${e.message}", e)
        }
    }

    fun updateUserStats(newStats: UserStats) {
        viewModelScope.launch {
            try {
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    Log.d(TAG, "Actualizando estadísticas del usuario")

                    databaseReference.child("userStats").child(currentUser.uid)
                        .setValue(newStats)
                        .addOnSuccessListener {
                            Log.d(TAG, "Estadísticas actualizadas exitosamente")
                            setupStatsData(newStats)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error al actualizar estadísticas: ${e.message}", e)
                            handleError("Error al actualizar estadísticas")
                        }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en updateUserStats: ${e.message}", e)
                handleError("Error al actualizar estadísticas")
            }
        }
    }

    private fun handleError(message: String) {
        _isLoading.value = false
        _errorMessage.value = message
        Log.e(TAG, message)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "DashboardViewModel destruido")
    }
}