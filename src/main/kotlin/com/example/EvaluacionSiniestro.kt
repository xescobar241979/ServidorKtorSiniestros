package com.example

import kotlinx.serialization.Serializable

@Serializable
data class ResultadoEvaluacion(
    val aprobado: Boolean,
    val mensaje: String,
    val detalles: Map<String, String> = emptyMap()
)

fun evaluarSiniestro(reporte: ReporteMedico): ResultadoEvaluacion {
    if (!reporte.clienteEnRedEnlace) {
        return ResultadoEvaluacion(false, "Rechazado: No es cliente Red Enlace")
    }

    if (reporte.enfermedadPosiblePreexistente &&
        reporte.fechaContratacion >= reporte.fechaInicioSintomas
    ) {
        return ResultadoEvaluacion(false, "Rechazado: Enfermedad preexistente")
    }

    if (!reporte.deduciblePagado) {
        return ResultadoEvaluacion(false, "Pendiente: Deducible no pagado")
    }

    val documentosRequeridos = listOf(
        "Solicitud de reclamación Red Enlace",
        "Identificación oficial",
        "Informe Médico",
        "Finiquito de la aseguradora primaria"
    )

    val faltantes = documentosRequeridos.filter { doc ->
        !reporte.documentosAdjuntos.any { it.contains(doc, ignoreCase = true) }
    }

    if (faltantes.isNotEmpty()) {
        return ResultadoEvaluacion(
            false,
            "Pendiente: Faltan documentos",
            mapOf("documentosFaltantes" to faltantes.joinToString(", "))
        )
    }

    return ResultadoEvaluacion(
        true,
        "✅ Aprobado automáticamente",
        mapOf(
            "montoAprobado" to "${reporte.gastoHospitalario - reporte.deducible}",
            "paciente" to (reporte.paciente ?: "No especificado")
        )
    )
}
