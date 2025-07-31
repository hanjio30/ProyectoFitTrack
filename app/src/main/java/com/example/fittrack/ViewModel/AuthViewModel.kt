package com.example.fittrack.viewmodel

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.fittrack.model.AuthResult
import com.example.fittrack.network.Callback
import com.example.fittrack.repository.AuthRepository

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    // LiveData para observar desde la Activity
    val authResult = MutableLiveData<AuthResult>()
    val registrationSuccess = MutableLiveData<Boolean>()
    val isLoading = MutableLiveData<Boolean>()
    val validationError = MutableLiveData<String>()

    fun isUserLoggedIn(): Boolean {
        return authRepository.getCurrentUser() != null
    }

    fun registerUser(fullName: String, email: String, password: String) {
        Log.d("AuthViewModel", "=== INICIANDO REGISTRO ===")
        Log.d("AuthViewModel", "registerUser llamado - Email: $email")

        // Validar datos
        val validationResult = validateRegistration(fullName, email, password)
        Log.d("AuthViewModel", "Validación completada. Es válida: ${validationResult.isValid}")

        if (!validationResult.isValid) {
            Log.d("AuthViewModel", "Validación falló: ${validationResult.errorMessage}")
            validationError.value = validationResult.errorMessage ?: "Error de validación"
            return
        }

        Log.d("AuthViewModel", "Validación pasó, iniciando loading")
        isLoading.value = true

        Log.d("AuthViewModel", "Llamando a authRepository.registerUser")
        authRepository.registerUser(fullName, email, password, object : Callback<AuthResult> {
            override fun onSuccess(result: AuthResult?) {
                Log.d("AuthViewModel", "=== CALLBACK onSuccess LLAMADO ===")
                Log.d("AuthViewModel", "Resultado recibido: $result")
                Log.d("AuthViewModel", "¿Resultado es null? ${result == null}")
                Log.d("AuthViewModel", "¿Resultado es exitoso? ${result?.isSuccess}")

                isLoading.value = false

                if (result != null && result.isSuccess) {
                    Log.d("AuthViewModel", "RESULTADO EXITOSO - Notificando registrationSuccess")
                    Log.d("AuthViewModel", "Configurando registrationSuccess.value = true")

                    // Registro exitoso - notificar para mostrar toast y regresar al login
                    registrationSuccess.value = true

                    Log.d("AuthViewModel", "registrationSuccess configurado. Valor actual: ${registrationSuccess.value}")
                } else {
                    Log.d("AuthViewModel", "RESULTADO NO EXITOSO")
                    Log.d("AuthViewModel", "Configurando authResult con error")

                    // Error en el registro
                    authResult.value = result ?: AuthResult(
                        isSuccess = false,
                        errorMessage = "Error desconocido en el registro"
                    )
                }

                Log.d("AuthViewModel", "=== FIN CALLBACK onSuccess ===")
            }

            override fun onFailed(exception: Exception) {
                Log.d("AuthViewModel", "=== CALLBACK onFailed LLAMADO ===")
                Log.d("AuthViewModel", "Error en registro: ${exception.message}")

                isLoading.value = false

                // Error en el registro
                authResult.value = AuthResult(
                    isSuccess = false,
                    errorMessage = exception.message
                )

                Log.d("AuthViewModel", "=== FIN CALLBACK onFailed ===")
            }
        })

        Log.d("AuthViewModel", "Callback configurado, esperando respuesta...")
    }

    fun loginUser(email: String, password: String) {
        Log.d("AuthViewModel", "=== INICIANDO LOGIN ===")

        // Validar datos
        val validationResult = validateLogin(email, password)
        if (!validationResult.isValid) {
            validationError.value = validationResult.errorMessage ?: "Error de validación"
            return
        }

        isLoading.value = true

        authRepository.loginUser(email, password, object : Callback<AuthResult> {
            override fun onSuccess(result: AuthResult?) {
                Log.d("AuthViewModel", "Login exitoso")
                isLoading.value = false
                result?.let {
                    authResult.value = it
                }
            }

            override fun onFailed(exception: Exception) {
                Log.d("AuthViewModel", "Error en login: ${exception.message}")
                isLoading.value = false
                authResult.value = AuthResult(
                    isSuccess = false,
                    errorMessage = exception.message
                )
            }
        })
    }

    // Método para resetear el estado de registro exitoso
    fun resetRegistrationSuccess() {
        Log.d("AuthViewModel", "resetRegistrationSuccess llamado")
        registrationSuccess.value = false
        Log.d("AuthViewModel", "registrationSuccess reseteado a: ${registrationSuccess.value}")
    }

    private fun validateRegistration(fullName: String, email: String, password: String): ValidationResult {
        if (fullName.trim().isEmpty()) {
            return ValidationResult(false, "Ingrese su nombre completo")
        }

        if (email.trim().isEmpty()) {
            return ValidationResult(false, "Ingrese su correo electrónico")
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return ValidationResult(false, "Ingrese un correo válido")
        }

        if (password.trim().isEmpty()) {
            return ValidationResult(false, "Ingrese una contraseña")
        }

        if (password.length < 6) {
            return ValidationResult(false, "La contraseña debe tener al menos 6 caracteres")
        }

        return ValidationResult(true)
    }

    private fun validateLogin(email: String, password: String): ValidationResult {
        if (email.trim().isEmpty()) {
            return ValidationResult(false, "Ingrese su correo electrónico")
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return ValidationResult(false, "Ingrese un correo válido")
        }

        if (password.trim().isEmpty()) {
            return ValidationResult(false, "Ingrese su contraseña")
        }

        return ValidationResult(true)
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )
}