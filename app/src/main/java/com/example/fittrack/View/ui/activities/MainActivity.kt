package com.example.fittrack.View.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.fittrack.R
import com.example.fittrack.viewmodel.AuthViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var viewFlipper: ViewFlipper
    private lateinit var btnStart: Button
    private lateinit var btnRegister: Button
    private lateinit var btnLogin: Button
    private lateinit var btnRegistro: Button
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etUsername: EditText
    private lateinit var etLoginPassword: EditText

    // ProgressBar opcional para mejor UX
    private var progressBar: ProgressBar? = null

    // ViewModel
    private lateinit var authViewModel: AuthViewModel

    companion object {
        private const val TAG = "MainActivity"
        private const val SCREEN_INITIAL = 0
        private const val SCREEN_LOGIN = 1
        private const val SCREEN_REGISTER = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "=== INICIANDO MainActivity ===")

        initializeViewModel()
        initializeViews()
        setupClickListeners()
        setupObservers()
        checkUserLoggedIn()

        Log.d(TAG, "=== MainActivity CONFIGURADA ===")
    }

    private fun initializeViewModel() {
        Log.d(TAG, "Inicializando ViewModel")
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
    }

    private fun initializeViews() {
        try {
            viewFlipper = findViewById(R.id.viewFlipper)

            // Botones
            btnStart = findViewById(R.id.btnStart)
            btnRegister = findViewById(R.id.btnRegister)
            btnLogin = findViewById(R.id.btnLogin)
            btnRegistro = findViewById(R.id.btnRegistro)

            // EditTexts de registro
            etFullName = findViewById(R.id.etFullName)
            etEmail = findViewById(R.id.etEmail)
            etPassword = findViewById(R.id.etPassword)

            // EditTexts de login
            etUsername = findViewById(R.id.etUsername)
            etLoginPassword = findViewById(R.id.etLoginPassword)


            Log.d(TAG, "Vistas inicializadas correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar vistas: ${e.message}", e)
        }
    }

    private fun checkUserLoggedIn() {
        try {
            // Si el usuario ya está logueado, ir directamente a ContentActivity
            if (authViewModel.isUserLoggedIn()) {
                Log.d(TAG, "Usuario ya autenticado, redirigiendo a ContentActivity")
                navigateToContentActivity()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar usuario logueado: ${e.message}", e)
        }
    }

    private fun setupClickListeners() {
        try {
            // Botón START inicial - va a pantalla de login
            btnStart.setOnClickListener {
                navigateToScreen(SCREEN_LOGIN)
            }

            // Botón REGISTRARSE - va a pantalla de registro
            btnRegistro.setOnClickListener {
                navigateToScreen(SCREEN_REGISTER)
            }

            // Botón de registro - llama al ViewModel
            btnRegister.setOnClickListener {
                performRegistration()
            }

            // Botón de login - llama al ViewModel
            btnLogin.setOnClickListener {
                performLogin()
            }

            Log.d(TAG, "Click listeners configurados")
        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar click listeners: ${e.message}", e)
        }
    }

    private fun performRegistration() {
        try {
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            Log.d(TAG, "=== BOTÓN REGISTRO PRESIONADO ===")
            Log.d(TAG, "Nombre: $fullName")
            Log.d(TAG, "Email: $email")
            Log.d(TAG, "Password length: ${password.length}")

            // Validación básica en la UI (opcional, el ViewModel también valida)
            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return
            }

            authViewModel.registerUser(fullName, email, password)
        } catch (e: Exception) {
            Log.e(TAG, "Error en registro: ${e.message}", e)
            Toast.makeText(this, "Error al procesar registro", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performLogin() {
        try {
            val email = etUsername.text.toString().trim()
            val password = etLoginPassword.text.toString().trim()

            Log.d(TAG, "=== BOTÓN LOGIN PRESIONADO ===")

            // Validación básica en la UI (opcional, el ViewModel también valida)
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return
            }

            authViewModel.loginUser(email, password)
        } catch (e: Exception) {
            Log.e(TAG, "Error en login: ${e.message}", e)
            Toast.makeText(this, "Error al procesar login", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        Log.d(TAG, "=== CONFIGURANDO OBSERVERS ===")

        // Observar el resultado de autenticación (para login Y errores de registro)
        authViewModel.authResult.observe(this) { result ->
            Log.d(TAG, "=== authResult OBSERVER ACTIVADO ===")
            Log.d(TAG, "Resultado: $result")
            Log.d(TAG, "¿Es exitoso? ${result.isSuccess}")

            if (result.isSuccess) {
                // Éxito en LOGIN - ir a ContentActivity
                Log.d(TAG, "LOGIN EXITOSO - Yendo a ContentActivity")
                showSuccessMessage("¡Bienvenido!")
                clearFields()
                navigateToContentActivity()
            } else {
                // Error en login O registro - mostrar mensaje
                Log.d(TAG, "ERROR: ${result.errorMessage}")
                showErrorMessage(result.errorMessage ?: "Error desconocido")
            }
        }

        // Observar registro exitoso (SOLO para registro exitoso)
        authViewModel.registrationSuccess.observe(this) { isSuccess ->
            Log.d(TAG, "=== registrationSuccess OBSERVER ACTIVADO ===")
            Log.d(TAG, "¿isSuccess? $isSuccess")

            if (isSuccess == true) {
                // Registro exitoso - mostrar toast y regresar al login
                Log.d(TAG, "REGISTRO EXITOSO - Mostrando toast y regresando al login")
                showSuccessMessage("Usuario registrado exitosamente. Ya puedes iniciar sesión")
                clearRegistrationFields()
                navigateToScreen(SCREEN_LOGIN)

                // Resetear el estado para evitar que se dispare de nuevo
                authViewModel.resetRegistrationSuccess()

                Log.d(TAG, "REGISTRO EXITOSO - Proceso completado")
            }
        }

        // Observar estado de carga
        authViewModel.isLoading.observe(this) { isLoading ->
            Log.d(TAG, "=== isLoading OBSERVER ACTIVADO ===")
            Log.d(TAG, "¿Está cargando? $isLoading")

            handleLoadingState(isLoading)
        }

        // Observar errores de validación
        authViewModel.validationError.observe(this) { errorMessage ->
            Log.d(TAG, "=== validationError OBSERVER ACTIVADO ===")
            Log.d(TAG, "Error de validación: $errorMessage")

            if (!errorMessage.isNullOrEmpty()) {
                showErrorMessage(errorMessage)
            }
        }

        Log.d(TAG, "=== OBSERVERS CONFIGURADOS ===")
    }

    private fun handleLoadingState(isLoading: Boolean) {
        try {
            if (isLoading) {
                showLoadingMessage("Procesando...")
                setButtonsEnabled(false)
                progressBar?.visibility = View.VISIBLE
            } else {
                setButtonsEnabled(true)
                progressBar?.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al manejar estado de carga: ${e.message}", e)
        }
    }

    private fun navigateToScreen(screenIndex: Int) {
        try {
            viewFlipper.displayedChild = screenIndex
            Log.d(TAG, "Navegado a pantalla: $screenIndex")
        } catch (e: Exception) {
            Log.e(TAG, "Error al navegar a pantalla $screenIndex: ${e.message}", e)
        }
    }

    private fun navigateToContentActivity() {
        try {
            startActivity(Intent(this, ContentActivity::class.java))
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error al navegar a ContentActivity: ${e.message}", e)
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        try {
            btnRegister.isEnabled = enabled
            btnLogin.isEnabled = enabled
        } catch (e: Exception) {
            Log.e(TAG, "Error al cambiar estado de botones: ${e.message}", e)
        }
    }

    private fun clearRegistrationFields() {
        try {
            etFullName.text.clear()
            etEmail.text.clear()
            etPassword.text.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Error al limpiar campos de registro: ${e.message}", e)
        }
    }

    private fun clearFields() {
        try {
            clearRegistrationFields()
            etUsername.text.clear()
            etLoginPassword.text.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Error al limpiar campos: ${e.message}", e)
        }
    }

    private fun showSuccessMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showLoadingMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        try {
            when (viewFlipper.displayedChild) {
                SCREEN_LOGIN -> {
                    // Si está en login, volver a pantalla inicial
                    navigateToScreen(SCREEN_INITIAL)
                }
                SCREEN_REGISTER -> {
                    // Si está en registro, volver a login
                    navigateToScreen(SCREEN_LOGIN)
                }
                else -> {
                    // Si está en pantalla inicial, salir de la app
                    super.onBackPressed()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en onBackPressed: ${e.message}", e)
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity destruida")
    }
}