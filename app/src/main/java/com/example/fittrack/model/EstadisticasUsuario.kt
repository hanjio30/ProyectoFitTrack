package com.example.fittrack.model

data class EstadisticasUsuario(
    val diasConsecutivos: Int = 0,
    val metasAlcanzadas: Int = 0,
    val mejorRacha: Int = 0,
    val promedioSemanal: Double = 0.0,
    val totalKilometros: Double = 0.0,
    val ultimaActualizacion: Long = System.currentTimeMillis()
)