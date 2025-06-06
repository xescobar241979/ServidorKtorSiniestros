package com.example

import kotlinx.serialization.Serializable

@Serializable
data class ReporteMedico(
    val clienteEnRedEnlace: Boolean,
    val fechaContratacion: String,
    val fechaInicioSintomas: String,
    val gastoHospitalario: Double,
    val deducible: Double,
    val deduciblePagado: Boolean,
    val enfermedadPosiblePreexistente: Boolean,
    val documentosAdjuntos: List<String>,
    val paciente: String? = null,
    val medico: String? = null,
    val fechaAdmision: String? = null,
    val fechaSalida: String? = null,
    val responsable: String? = null,
    val nombreEnCertificado: String?,
    val nombreExtraidoDocumento: String?,
    val fechaAntiguedadCertificado: String? = null,
    val fechaInicioPadecimientoInforme: String? = null
)
