package com.example

import java.time.LocalDate
import com.example.ResultadoEvaluacion



suspend fun main() {
    println("🔵 Iniciando prueba de siniestro...")

    // Creamos un siniestro de prueba
    val siniestro = Siniestro(
        descripcionPadecimiento = "Sangrado uterino anormal secundario a pólipo endometrial",
        clienteEnRedEnlace = true,
        enfermedadPosiblePreexistente = false,
        fechaContratacion = LocalDate.of(2023, 1, 1),
        fechaInicioSintomas = LocalDate.of(2024, 4, 10),
        deduciblePagado = true,
        documentosAdjuntos = listOf(
            "Solicitud de reclamación",
            "Identificación oficial",
            "Informe Médico",
            "Finiquito de la aseguradora primaria",
            "Corte de caja",
            "Certificado",
            "Estado de cuenta"
        ),
        gastoHospitalario = 10000.0,
        deducible = 5000.0,
        paciente = "Juan Pérez"
    )

    // Evaluamos el siniestro
    val resultado = evaluarSiniestro(siniestro)

    println("📋 Resultado Evaluación:")
    println("✅ Aprobado: ${resultado.aprobado}")
    println("📝 Mensaje: ${resultado.mensaje}")
    resultado.detalles?.let { detalles ->
        if (detalles.isNotEmpty()) {
            println("📄 Detalles:")
            detalles.forEach { (key, value) ->
                println("- $key: $value")
            }
        }
    }
}

// ✅ Evaluador básico de siniestros
fun evaluarSiniestro(siniestro: Siniestro): ResultadoEvaluacion {
    if (!siniestro.clienteEnRedEnlace) {
        return ResultadoEvaluacion(false, "❌ Rechazado: No es cliente Red Enlace")
    }

    if (siniestro.enfermedadPosiblePreexistente && siniestro.fechaContratacion >= siniestro.fechaInicioSintomas) {
        return ResultadoEvaluacion(false, "❌ Rechazado: Enfermedad preexistente")
    }

    if (!siniestro.deduciblePagado) {
        return ResultadoEvaluacion(false, "❌ Pendiente: Deducible no pagado")
    }

    val documentosRequeridos = listOf(
        "Solicitud de reclamación",
        "Identificación oficial",
        "Informe Médico",
        "Finiquito de la aseguradora primaria",
        "Corte de caja",
        "Certificado",
        "Estado de cuenta"
    )

    val faltantes = documentosRequeridos.filterNot { requerido ->
        siniestro.documentosAdjuntos.any { adjunto ->
            adjunto.contains(requerido, ignoreCase = true)
        }
    }

    if (faltantes.isNotEmpty()) {
        return ResultadoEvaluacion(
            aprobado = false,
            mensaje = "❗ Pendiente: Faltan documentos",
            detalles = mapOf("documentosFaltantes" to faltantes.joinToString(", "))
        )
    }

    return ResultadoEvaluacion(
        aprobado = true,
        mensaje = "✅ Aprobado automáticamente",
        detalles = mapOf(
            "montoAprobado" to "${siniestro.gastoHospitalario - siniestro.deducible}",
            "paciente" to (siniestro.paciente ?: "No especificado")
        )
    )
}
