package com.example.fittrack.ViewModel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.fittrack.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.InputStream

class PerfilViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _profileData = MutableLiveData<UserProfile>()
    val profileData: LiveData<UserProfile> = _profileData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _saveResult = MutableLiveData<Result<String>>()
    val saveResult: LiveData<Result<String>> = _saveResult

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _isLoading.value = true

            firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    _isLoading.value = false
                    if (document.exists()) {
                        val profile = document.toObject(UserProfile::class.java)
                        profile?.let {
                            _profileData.value = it
                        }
                    } else {
                        // Si no existe el perfil, crear uno vacío con el userId
                        _profileData.value = UserProfile(userId = currentUser.uid)
                    }
                }
                .addOnFailureListener { exception ->
                    _isLoading.value = false
                    _saveResult.value = Result.failure(exception)
                }
        }
    }

    fun saveProfile(context: Context, gender: String, age: String, weight: String, height: String, imageUri: Uri? = null) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _saveResult.value = Result.failure(Exception("Usuario no autenticado"))
            return
        }

        // Validar datos
        val validationResult = validateProfileData(age, weight, height)
        if (!validationResult.isSuccess) {
            _saveResult.value = validationResult
            return
        }

        _isLoading.value = true

        val currentProfile = _profileData.value ?: UserProfile(userId = currentUser.uid)

        // Convertir imagen a Base64 si existe
        var imageBase64 = currentProfile.profileImageBase64
        if (imageUri != null) {
            try {
                imageBase64 = convertImageToBase64(context, imageUri)
            } catch (e: Exception) {
                _isLoading.value = false
                _saveResult.value = Result.failure(Exception("Error al procesar la imagen: ${e.message}"))
                return
            }
        }

        val updatedProfile = currentProfile.copy(
            gender = gender,
            age = age.toInt(),
            weight = weight.toDouble(),
            height = height.toInt(),
            profileImageBase64 = imageBase64,
            updatedAt = System.currentTimeMillis()
        )

        saveProfileToFirestore(updatedProfile)
    }

    private fun convertImageToBase64(context: Context, imageUri: Uri): String {
        val inputStream: InputStream = context.contentResolver.openInputStream(imageUri)
            ?: throw Exception("No se pudo abrir la imagen")

        // Redimensionar la imagen para evitar que sea muy grande
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val resizedBitmap = resizeBitmap(bitmap, 300, 300) // Máximo 300x300px

        val byteArrayOutputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream) // 80% calidad
        val byteArray = byteArrayOutputStream.toByteArray()

        // Verificar que no sea muy grande (máximo 1MB)
        if (byteArray.size > 1024 * 1024) {
            throw Exception("La imagen es muy grande. Máximo 1MB.")
        }

        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val aspectRatio = width.toFloat() / height.toFloat()

        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxWidth
            newHeight = (maxWidth / aspectRatio).toInt()
        } else {
            newHeight = maxHeight
            newWidth = (maxHeight * aspectRatio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun saveProfileToFirestore(profile: UserProfile) {
        firestore.collection("users")
            .document(profile.userId)
            .set(profile)
            .addOnSuccessListener {
                _isLoading.value = false
                _profileData.value = profile
                _saveResult.value = Result.success("Perfil guardado exitosamente")
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                _saveResult.value = Result.failure(exception)
            }
    }

    private fun validateProfileData(age: String, weight: String, height: String): Result<String> {
        return try {
            if (age.isEmpty() || weight.isEmpty() || height.isEmpty()) {
                return Result.failure(Exception("Por favor completa todos los campos"))
            }

            val ageInt = age.toInt()
            val weightDouble = weight.toDouble()
            val heightInt = height.toInt()

            when {
                ageInt < 1 || ageInt > 120 -> Result.failure(Exception("La edad debe estar entre 1 y 120 años"))
                weightDouble < 20 || weightDouble > 500 -> Result.failure(Exception("El peso debe estar entre 20 y 500 kg"))
                heightInt < 50 || heightInt > 250 -> Result.failure(Exception("La altura debe estar entre 50 y 250 cm"))
                else -> Result.success("Datos válidos")
            }
        } catch (e: NumberFormatException) {
            Result.failure(Exception("Por favor ingresa valores numéricos válidos"))
        }
    }

    fun deleteProfileImage() {
        val currentProfile = _profileData.value
        if (currentProfile != null && currentProfile.profileImageBase64.isNotEmpty()) {
            _isLoading.value = true

            // Actualizar perfil sin la imagen
            val updatedProfile = currentProfile.copy(
                profileImageBase64 = "",
                updatedAt = System.currentTimeMillis()
            )
            saveProfileToFirestore(updatedProfile)
        }
    }
}