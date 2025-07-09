package com.example.fittrack

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity

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

    // Sistema de autenticación en memoria
    private val registeredUsers = mutableMapOf<String, UserData>()

    data class UserData(
        val fullName: String,
        val email: String,
        val password: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
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
    }

    private fun setupClickListeners() {
        // Botón START inicial - va a pantalla de login
        btnStart.setOnClickListener {
            viewFlipper.displayedChild = 1 // Mostrar pantalla de login
        }

        // Botón REGISTRARSE - va a pantalla de registro
        btnRegistro.setOnClickListener {
            viewFlipper.displayedChild = 2 // Mostrar pantalla de registro
        }

        // Botón de registro - guarda datos y vuelve a login
        btnRegister.setOnClickListener {
            if (validateRegistration()) {
                registerUser()
                viewFlipper.displayedChild = 1 // Volver a pantalla de login
                Toast.makeText(this, "Registro exitoso. Ahora puedes iniciar sesión", Toast.LENGTH_SHORT).show()
                clearRegistrationFields()
            }
        }

        // Botón de login - autentica y va a ContentActivity
        btnLogin.setOnClickListener {
            if (validateLogin()) {
                if (authenticateUser()) {
                    Toast.makeText(this, "Login exitoso", Toast.LENGTH_SHORT).show()
                    // Ir a ContentActivity
                    startActivity(Intent(this, ContentActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun validateRegistration(): Boolean {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (fullName.isEmpty()) {
            etFullName.error = "Ingrese su nombre completo"
            return false
        }

        if (email.isEmpty()) {
            etEmail.error = "Ingrese su correo electrónico"
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Ingrese un correo válido"
            return false
        }

        if (password.isEmpty()) {
            etPassword.error = "Ingrese una contraseña"
            return false
        }

        if (password.length < 6) {
            etPassword.error = "La contraseña debe tener al menos 6 caracteres"
            return false
        }

        // Verificar si el usuario ya existe
        if (registeredUsers.containsKey(fullName)) {
            etFullName.error = "Este usuario ya existe"
            return false
        }

        // Verificar si el email ya existe
        if (registeredUsers.values.any { it.email == email }) {
            etEmail.error = "Este correo ya está registrado"
            return false
        }

        return true
    }

    private fun validateLogin(): Boolean {
        val username = etUsername.text.toString().trim()
        val password = etLoginPassword.text.toString().trim()

        if (username.isEmpty()) {
            etUsername.error = "Ingrese su nombre de usuario"
            return false
        }

        if (password.isEmpty()) {
            etLoginPassword.error = "Ingrese su contraseña"
            return false
        }

        return true
    }

    private fun registerUser() {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        val userData = UserData(fullName, email, password)
        registeredUsers[fullName] = userData
    }

    private fun authenticateUser(): Boolean {
        val username = etUsername.text.toString().trim()
        val password = etLoginPassword.text.toString().trim()

        val userData = registeredUsers[username]
        return userData != null && userData.password == password
    }

    private fun clearRegistrationFields() {
        etFullName.text.clear()
        etEmail.text.clear()
        etPassword.text.clear()
    }

    override fun onBackPressed() {
        when (viewFlipper.displayedChild) {
            1 -> {
                // Si está en login, volver a pantalla inicial
                viewFlipper.displayedChild = 0
            }
            2 -> {
                // Si está en registro, volver a login
                viewFlipper.displayedChild = 1
            }
            else -> {
                // Si está en pantalla inicial, salir de la app
                super.onBackPressed()
            }
        }
    }
}