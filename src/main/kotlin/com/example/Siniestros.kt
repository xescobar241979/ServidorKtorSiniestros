package com.example

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import java.time.LocalDate

@Serializable
data class Siniestro(
    val descripcionPadecimiento: String,
    val clienteEnRedEnlace: Boolean,
    val enfermedadPosiblePreexistente: Boolean,
    val deduciblePagado: Boolean,
    val documentosAdjuntos: List<String>,
    @Contextual val fechaContratacion: LocalDate,
    @Contextual val fechaInicioSintomas: LocalDate,
    val gastoHospitalario: Double,
    val deducible: Double,
    val paciente: String? = null
)
