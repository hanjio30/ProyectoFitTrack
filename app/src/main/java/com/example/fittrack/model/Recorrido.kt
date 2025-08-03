package com.example.fittrack.model

data class Recorrido(
    val id: String = "",
    val userId: String = "",
    val fecha: String = "",
    val distanciaKm: Double = 0.0,
    val duracionMinutos: Int = 0,
    val caloriasQuemadas: Int = 0,
    val origen: String = "",
    val destino: String = "",
    val createdAt: Long = 0L
)