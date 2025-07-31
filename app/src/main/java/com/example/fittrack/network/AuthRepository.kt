package com.example.fittrack.repository

import android.util.Log
import com.example.fittrack.model.AuthResult
import com.example.fittrack.model.UserData
import com.example.fittrack.network.Callback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class AuthRepository {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("users")

    init {
        // Verificar conexión a Firebase Database
        Log.d("AuthRepository", "Firebase Database URL: ${FirebaseDatabase.getInstance().reference.toString()}")
    }

    fun getCurrentUser() = firebaseAuth.currentUser

    fun registerUser(
        fullName: String,
        email: String,
        password: String,
        callback: Callback<AuthResult>
    ) {
        Log.d("AuthRepository", "Iniciando registro para: $email")

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AuthRepository", "Usuario creado exitosamente en Auth")
                    val user = firebaseAuth.currentUser
                    user?.let { firebaseUser ->
                        val userData = UserData(
                            fullName = fullName,
                            email = email,
                            uid = firebaseUser.uid
                        )

                        Log.d("AuthRepository", "Guardando datos en Database para UID: ${firebaseUser.uid}")

                        // Guardar datos adicionales en Firebase Database
                        databaseReference.child(firebaseUser.uid).setValue(userData)
                            .addOnSuccessListener {
                                Log.d("AuthRepository", "Datos guardados exitosamente en Database")

                                // Después del registro exitoso, cerrar sesión para que regrese al login
                                firebaseAuth.signOut()
                                Log.d("AuthRepository", "Usuario deslogueado, llamando callback de éxito")

                                // IMPORTANTE: Llamar al callback en el hilo principal
                                callback.onSuccess(
                                    AuthResult(
                                        isSuccess = true,
                                        user = userData
                                    )
                                )
                            }
                            .addOnFailureListener { exception ->
                                Log.e("AuthRepository", "Error al guardar en Database: ${exception.message}")
                                callback.onFailed(Exception("Error al guardar datos: ${exception.message}"))
                            }
                    } ?: run {
                        Log.e("AuthRepository", "Usuario de Firebase es null después de registro exitoso")
                        callback.onFailed(Exception("Error: Usuario no encontrado después del registro"))
                    }
                } else {
                    Log.e("AuthRepository", "Error en createUserWithEmailAndPassword: ${task.exception?.message}")
                    val errorMessage = when {
                        task.exception?.message?.contains("email address is already in use") == true ->
                            "Este correo ya está registrado"
                        task.exception?.message?.contains("weak password") == true ->
                            "La contraseña es muy débil"
                        task.exception?.message?.contains("invalid email") == true ->
                            "El correo electrónico no es válido"
                        else -> "Error en el registro: ${task.exception?.message}"
                    }
                    callback.onFailed(Exception(errorMessage))
                }
            }
    }

    fun loginUser(
        email: String,
        password: String,
        callback: Callback<AuthResult>
    ) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    user?.let { firebaseUser ->
                        val userData = UserData(
                            fullName = "", // Se puede obtener de la BD si es necesario
                            email = firebaseUser.email ?: "",
                            uid = firebaseUser.uid
                        )

                        callback.onSuccess(
                            AuthResult(
                                isSuccess = true,
                                user = userData
                            )
                        )
                    }
                } else {
                    val errorMessage = when {
                        task.exception?.message?.contains("user-not-found", ignoreCase = true) == true ||
                                task.exception?.message?.contains("user not found", ignoreCase = true) == true ->
                            "Usuario no encontrado"

                        task.exception?.message?.contains("wrong-password", ignoreCase = true) == true ||
                                task.exception?.message?.contains("wrong password", ignoreCase = true) == true ->
                            "Contraseña incorrecta"

                        task.exception?.message?.contains("invalid-email", ignoreCase = true) == true ||
                                task.exception?.message?.contains("invalid email", ignoreCase = true) == true ||
                                task.exception?.message?.contains("badly formatted", ignoreCase = true) == true ->
                            "Correo electrónico no válido"

                        task.exception?.message?.contains("user-disabled", ignoreCase = true) == true ||
                                task.exception?.message?.contains("user disabled", ignoreCase = true) == true ->
                            "Usuario deshabilitado"

                        task.exception?.message?.contains("invalid-credential", ignoreCase = true) == true ||
                                task.exception?.message?.contains("auth credential", ignoreCase = true) == true ||
                                task.exception?.message?.contains("supplied auth", ignoreCase = true) == true ->
                            "Credenciales incorrectas. Verifica tu email y contraseña"

                        task.exception?.message?.contains("network", ignoreCase = true) == true ->
                            "Error de conexión. Verifica tu internet"

                        task.exception?.message?.contains("too-many-requests", ignoreCase = true) == true ->
                            "Demasiados intentos. Intenta más tarde"

                        else -> "Error en el login. Verifica tus credenciales"
                    }
                    callback.onFailed(Exception(errorMessage))
                }
            }
    }
}