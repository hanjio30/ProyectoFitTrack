package com.example.fittrack.model

data class MetaDiaria(
    val id: String = "",
    val userId: String = "",
    val fecha: String = "", // formato: "2025-08-03"
    val metaKilometros: Double = 10.0,
    val progresoActual: Double = 0.0,
    val porcentajeCompletado: Int = 0,
    val puntosGanados: Int = 0,
    val metaAlcanzada: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)