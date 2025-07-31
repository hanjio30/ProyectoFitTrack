package com.example.fittrack.ViewModel

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ContentViewModel : ViewModel() {

    // Firebase
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("users")

    // LiveData para la UI
    private val _isFirstTime = MutableLiveData<Boolean>()
    val isFirstTime: LiveData<Boolean> = _isFirstTime

    private val _currentUserName = MutableLiveData<String>()
    val currentUserName: LiveData<String> = _currentUserName

    private val _currentUserEmail = MutableLiveData<String>()
    val currentUserEmail: LiveData<String> = _currentUserEmail

    private val _showOnboarding = MutableLiveData<Boolean>()
    val showOnboarding: LiveData<Boolean> = _showOnboarding

    private val _showMainContent = MutableLiveData<Boolean>()
    val showMainContent: LiveData<Boolean> = _showMainContent

    private val _refreshDashboard = MutableLiveData<Boolean>()
    val refreshDashboard: LiveData<Boolean> = _refreshDashboard

    private val _currentFragment = MutableLiveData<FragmentType>()
    val currentFragment: LiveData<FragmentType> = _currentFragment

    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent

    // Constantes
    companion object {
        private const val TAG = "ContentViewModel"
        const val PREF_NAME = "FitTrackPrefs"
        const val KEY_FIRST_TIME = "is_first_time"
    }

    // Enum para tipos de fragmentos
    enum class FragmentType {
        DASHBOARD, ESTADISTICAS, MAP, PERFIL, RECORRIDO
    }

    // Data class para datos del usuario
    data class UserData(
        val fullName: String = "",
        val email: String = "",
        val uid: String = ""
    )

    fun initializeApp(sharedPreferences: SharedPreferences) {
        try {
            Log.d(TAG, "=== INICIALIZANDO ContentViewModel ===")

            val isFirstTime = sharedPreferences.getBoolean(KEY_FIRST_TIME, true)
            Log.d(TAG, "¿Es primera vez? $isFirstTime")

            _isFirstTime.value = isFirstTime

            if (isFirstTime) {
                _showOnboarding.value = true
                _showMainContent.value = false
            } else {
                _showOnboarding.value = false
                _showMainContent.value = true
                loadUserData()
                _currentFragment.value = FragmentType.DASHBOARD
            }

            Log.d(TAG, "=== ContentViewModel INICIALIZADO EXITOSAMENTE ===")
        } catch (e: Exception) {
            Log.e(TAG, "Error en initializeApp: ${e.message}", e)
            // En caso de error, mostrar contenido principal
            _showOnboarding.value = false
            _showMainContent.value = true
            setDefaultUserData()
        }
    }

    fun onOnboardingComplete(sharedPreferences: SharedPreferences) {
        try {
            Log.d(TAG, "Onboarding completado")

            // Marcar que ya no es la primera vez
            sharedPreferences.edit().putBoolean(KEY_FIRST_TIME, false).apply()
            _isFirstTime.value = false

            // Mostrar contenido principal
            _showOnboarding.value = false
            _showMainContent.value = true

            loadUserData()
            _currentFragment.value = FragmentType.DASHBOARD

        } catch (e: Exception) {
            Log.e(TAG, "Error en onOnboardingComplete: ${e.message}", e)
        }
    }

    fun loadUserData() {
        try {
            Log.d(TAG, "Cargando datos del usuario")

            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                Log.d(TAG, "Usuario autenticado encontrado: ${currentUser.email}")

                databaseReference.child(currentUser.uid)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            try {
                                if (snapshot.exists()) {
                                    val userData = snapshot.getValue(UserData::class.java)
                                    userData?.let {
                                        Log.d(TAG, "Datos del usuario obtenidos: ${it.fullName}")
                                        _currentUserName.value = it.fullName
                                        _currentUserEmail.value = it.email
                                        _refreshDashboard.value = true
                                    }
                                } else {
                                    Log.d(TAG, "No hay datos en la base de datos, usando datos de Auth")
                                    _currentUserName.value = currentUser.displayName ?: "Usuario"
                                    _currentUserEmail.value = currentUser.email ?: "usuario@ejemplo.com"
                                    _refreshDashboard.value = true
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error en onDataChange: ${e.message}", e)
                                setDefaultUserData(currentUser.email)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(TAG, "Error al cargar datos del usuario: ${error.message}")
                            setDefaultUserData(currentUser.email)
                        }
                    })
            } else {
                Log.w(TAG, "No hay usuario autenticado")
                setDefaultUserData()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error en loadUserData: ${e.message}", e)
            setDefaultUserData()
        }
    }

    private fun setDefaultUserData(email: String? = null) {
        try {
            _currentUserName.value = "Usuario"
            _currentUserEmail.value = email ?: "usuario@ejemplo.com"
            _refreshDashboard.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Error en setDefaultUserData: ${e.message}", e)
        }
    }

    fun navigateToFragment(fragmentType: FragmentType) {
        try {
            Log.d(TAG, "Navegando a: $fragmentType")
            _currentFragment.value = fragmentType
        } catch (e: Exception) {
            Log.e(TAG, "Error en navigateToFragment: ${e.message}", e)
        }
    }

    fun logout(sharedPreferences: SharedPreferences) {
        try {
            Log.d(TAG, "Cerrando sesión")

            // Cerrar sesión de Firebase
            firebaseAuth.signOut()

            // Resetear SharedPreferences
            sharedPreferences.edit().putBoolean(KEY_FIRST_TIME, true).apply()

            _logoutEvent.value = true

        } catch (e: Exception) {
            Log.e(TAG, "Error al cerrar sesión: ${e.message}", e)
            _logoutEvent.value = true
        }
    }

    fun getCurrentUserName(): String {
        return _currentUserName.value ?: "Usuario"
    }

    fun resetRefreshDashboard() {
        _refreshDashboard.value = false
    }

    fun resetLogoutEvent() {
        _logoutEvent.value = false
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ContentViewModel destruido")
    }
}