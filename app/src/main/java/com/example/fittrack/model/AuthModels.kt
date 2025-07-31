package com.example.fittrack.model

data class UserData(
    val fullName: String = "",
    val email: String = "",
    val uid: String = ""
)

data class AuthResult(
    val isSuccess: Boolean,
    val user: UserData? = null,
    val errorMessage: String? = null
)