package com.example.fittrack.model

data class UserProfile(
    val userId: String = "",
    val gender: String = "",
    val age: Int = 0,
    val weight: Double = 0.0,
    val height: Int = 0,
    val profileImageBase64: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Constructor vacío requerido por Firestore
    constructor() : this("", "", 0, 0.0, 0, "", 0L, 0L)
}