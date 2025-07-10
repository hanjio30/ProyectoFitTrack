package com.example.fittrack

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

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

    // Firebase Auth y Database
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    data class UserData(
        val fullName: String = "",
        val email: String = "",
        val uid: String = ""
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeFirebase()
        initializeViews()
        setupClickListeners()
        checkUserLoggedIn()
    }

    private fun initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().getReference("users")
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

    private fun checkUserLoggedIn() {
        // Si el usuario ya está logueado, ir directamente a ContentActivity
        if (firebaseAuth.currentUser != null) {
            startActivity(Intent(this, ContentActivity::class.java))
            finish()
        }
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

        // Botón de registro - registra con Firebase Auth
        btnRegister.setOnClickListener {
            if (validateRegistration()) {
                registerUserWithFirebase()
            }
        }

        // Botón de login - autentica con Firebase Auth
        btnLogin.setOnClickListener {
            if (validateLogin()) {
                loginUserWithFirebase()
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

        return true
    }

    private fun validateLogin(): Boolean {
        val email = etUsername.text.toString().trim()
        val password = etLoginPassword.text.toString().trim()

        if (email.isEmpty()) {
            etUsername.error = "Ingrese su correo electrónico"
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etUsername.error = "Ingrese un correo válido"
            return false
        }

        if (password.isEmpty()) {
            etLoginPassword.error = "Ingrese su contraseña"
            return false
        }

        return true
    }

    private fun registerUserWithFirebase() {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Mostrar progreso
        Toast.makeText(this, "Registrando usuario...", Toast.LENGTH_SHORT).show()

        // Crear usuario con Firebase Auth
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registro exitoso
                    val user = firebaseAuth.currentUser
                    user?.let { firebaseUser ->
                        // Guardar datos adicionales en Firebase Database
                        val userData = UserData(
                            fullName = fullName,
                            email = email,
                            uid = firebaseUser.uid
                        )

                        // Guardar en la base de datos
                        databaseReference.child(firebaseUser.uid).setValue(userData)
                            .addOnSuccessListener {
                                // Todo exitoso - ir directamente a ContentActivity
                                Toast.makeText(this, "Registro exitoso. Bienvenido!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, ContentActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(this, "Error al guardar datos: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    // Error en el registro
                    val errorMessage = when {
                        task.exception?.message?.contains("email address is already in use") == true ->
                            "Este correo ya está registrado"
                        task.exception?.message?.contains("weak password") == true ->
                            "La contraseña es muy débil"
                        task.exception?.message?.contains("invalid email") == true ->
                            "El correo electrónico no es válido"
                        else -> "Error en el registro: ${task.exception?.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun loginUserWithFirebase() {
        val email = etUsername.text.toString().trim()
        val password = etLoginPassword.text.toString().trim()

        // Mostrar progreso
        Toast.makeText(this, "Iniciando sesión...", Toast.LENGTH_SHORT).show()

        // Autenticar con Firebase Auth
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login exitoso
                    Toast.makeText(this, "Login exitoso", Toast.LENGTH_SHORT).show()

                    // Ir a ContentActivity
                    startActivity(Intent(this, ContentActivity::class.java))
                    finish()
                } else {
                    // Error en el login
                    val errorMessage = when {
                        task.exception?.message?.contains("user not found") == true ->
                            "Usuario no encontrado"
                        task.exception?.message?.contains("wrong password") == true ->
                            "Contraseña incorrecta"
                        task.exception?.message?.contains("invalid email") == true ->
                            "Correo electrónico no válido"
                        task.exception?.message?.contains("user disabled") == true ->
                            "Usuario deshabilitado"
                        else -> "Error en el login: ${task.exception?.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
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