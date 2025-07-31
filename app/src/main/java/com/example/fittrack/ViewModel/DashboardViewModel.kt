package com.example.fittrack.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel : ViewModel() {

    // Firebase
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().getReference()

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

    fun initializeDashboard(userName: String? = null) {
        try {
            Log.d(TAG, "=== INICIALIZANDO DashboardViewModel ===")

            _isLoading.value = true

            if (!userName.isNullOrEmpty()) {
                // Si se pasa un nombre, usarlo directamente
                setupUserData(userName)
            } else {
                // Si no, obtener desde Firebase
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

                    // Cargar estadísticas desde Firebase
                    databaseReference.child("userStats").child(currentUser.uid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                try {
                                    if (snapshot.exists()) {
                                        val userStats = snapshot.getValue(UserStats::class.java)
                                        setupStatsData(userStats ?: UserStats())
                                    } else {
                                        Log.d(TAG, "No hay estadísticas, usando datos de ejemplo")
                                        setupDefaultStats()
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error al procesar estadísticas: ${e.message}", e)
                                    setupDefaultStats()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e(TAG, "Error al cargar estadísticas: ${error.message}")
                                setupDefaultStats()
                            }
                        })
                } else {
                    setupDefaultStats()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en loadUserStats: ${e.message}", e)
                setupDefaultStats()
            }
        }
    }

    private fun setupStatsData(userStats: UserStats) {
        try {
            _caloriesValue.value = "${userStats.calories} kcal"
            _activityValue.value = "${userStats.activityMinutes} min"
            _stepsValue.value = userStats.steps.toString()
            _hydrationValue.value = "${userStats.hydrationLiters} lit."
            _streakValue.value = "${userStats.streak} días"
            _distanceValue.value = "${userStats.distanceKm} km"
            _goalValue.value = "${userStats.goalProgress}%"

            _isLoading.value = false
            Log.d(TAG, "Estadísticas configuradas desde Firebase")
        } catch (e: Exception) {
            Log.e(TAG, "Error en setupStatsData: ${e.message}", e)
            setupDefaultStats()
        }
    }

    private fun setupDefaultStats() {
        try {
            // Datos de ejemplo más realistas
            _caloriesValue.value = "25 kcal"
            _activityValue.value = "45 min"
            _stepsValue.value = "58"
            _hydrationValue.value = "1.2 lit."
            _streakValue.value = "20 días"
            _distanceValue.value = "1.5 km"
            _goalValue.value = "75%"

            _isLoading.value = false
            Log.d(TAG, "Datos de ejemplo configurados")
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