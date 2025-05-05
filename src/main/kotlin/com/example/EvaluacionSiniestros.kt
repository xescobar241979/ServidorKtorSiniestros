package com.example

import com.redenlace.siniestros.integrations.OpenAiService
import com.redenlace.siniestros.evaluation.PadecimientoEvaluator
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class ResultadoEvaluacion(
    val aprobado: Boolean,
    val mensaje: String,
    val detalles: Map<String, String>? = null
)

fun interface Logger {
    fun log(msg: String)
}

// ✅ Esta función valida condiciones del siniestro y nombre con OpenAI
suspend fun evaluarReporteMedico(
    reporte: ReporteMedico,
    openAiService: OpenAiService
): ResultadoEvaluacion {

    if (!reporte.clienteEnRedEnlace) {
        return ResultadoEvaluacion(
            aprobado = false,
            mensaje = "❌ Rechazado: No es cliente Red Enlace"
        )
    }

    if (reporte.enfermedadPosiblePreexistente && reporte.fechaContratacion >= reporte.fechaInicioSintomas) {
        return ResultadoEvaluacion(
            aprobado = false,
            mensaje = "❌ Rechazado: Enfermedad preexistente"
        )
    }

    if (!reporte.deduciblePagado) {
        return ResultadoEvaluacion(
            aprobado = false,
            mensaje = "❌ Pendiente: Deducible no pagado"
        )
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
        reporte.documentosAdjuntos.any { adjunto -> adjunto.contains(requerido, ignoreCase = true) }
    }

    if (faltantes.isNotEmpty()) {
        return ResultadoEvaluacion(
            aprobado = false,
            mensaje = "❗ Pendiente: Faltan documentos",
            detalles = mapOf("documentosFaltantes" to faltantes.joinToString(", "))
        )
    }

    // ✅ Validación del nombre del asegurado con OpenAI
    val nombreCertificado = reporte.nombreEnCertificado ?: ""
    val nombreDocumento = reporte.nombreExtraidoDocumento ?: ""

    if (!reporte.fechaAntiguedadCertificado.isNullOrBlank() && !reporte.fechaInicioPadecimientoInforme.isNullOrBlank()) {
        val resultadoFechas = openAiService.validarFechasSiniestroConOpenAI(
            reporte.fechaAntiguedadCertificado,
            reporte.fechaInicioPadecimientoInforme
        )

        if (resultadoFechas.contains("❌")) {
            return ResultadoEvaluacion(
                aprobado = false,
                mensaje = "❌ Fechas inconsistentes: $resultadoFechas"
            )
        }

        if (resultadoFechas.contains("⚠️")) {
            return ResultadoEvaluacion(
                aprobado = false,
                mensaje = "⚠️ Fechas sospechosas: $resultadoFechas"
            )
        }
    }

    if (nombreCertificado.isNotBlank() && nombreDocumento.isNotBlank()) {
        val resultadoNombre = openAiService.validarNombreConOpenAI(nombreCertificado, nombreDocumento)

        if (!resultadoNombre.contains("sí", ignoreCase = true)) {
            return ResultadoEvaluacion(
                aprobado = false,
                mensaje = resultadoNombre
            )
        }
    }

    return ResultadoEvaluacion(
        aprobado = true,
        mensaje = "✅ Aprobado automáticamente",
        detalles = mapOf(
            "montoAprobado" to "${reporte.gastoHospitalario - reporte.deducible}",
            "paciente" to (reporte.paciente ?: "No especificado")
        )
    )
}

// ✅ Esta función usa OpenAI para decidir si el siniestro es procedente
suspend fun evaluarSiniestroConOpenAI(diagnostico: String, openAiService: OpenAiService): ResultadoEvaluacion {
    return try {
        val evaluator = PadecimientoEvaluator(openAiService)
        val resultado = evaluator.evaluarPadecimientoConPeriodoDeEspera(diagnostico)

        if (resultado.equals("No aplica", ignoreCase = true)) {
            println("✅ $diagnostico no requiere periodo de espera.")
            ResultadoEvaluacion(
                aprobado = true,
                mensaje = "✅ Siniestro procedente (sin periodo de espera)"
            )
        } else {
            println("⛔ $diagnostico requiere periodo de espera. Categoría: $resultado")
            ResultadoEvaluacion(
                aprobado = false,
                mensaje = "⛔ Requiere periodo de espera: $resultado"
            )
        }
    } catch (e: Exception) {
        ResultadoEvaluacion(
            aprobado = false,
            mensaje = "❌ Error al evaluar siniestro: ${e.message}",
            detalles = null
        )
    }
}

// ✅ Simula una evaluación desde un archivo de reporte JSON
suspend fun analizarYValidarDocumentos(
    archivos: List<File>,
    openAiService: OpenAiService
): ResultadoOCR {
    println("🔍 Procesando documentos para evaluación...")

    val archivoReporte = File("src/main/resources/reporte_sini.json")
    if (!archivoReporte.exists()) {
        return ResultadoOCR(
            procede = "false",
            razon = "❌ Archivo de reporte no encontrado.",
            detalles = listOf("El archivo reporte_sini.json no está disponible en resources.")
        )
    }

    val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    val reporte = json.decodeFromString<ReporteMedico>(archivoReporte.readText())

    val evaluacion = evaluarReporteMedico(reporte, openAiService)

    println("📋 Resultado evaluación: ${evaluacion.mensaje}")

    return ResultadoOCR(
        procede = when {
            evaluacion.mensaje.contains("Pendiente", ignoreCase = true) -> "pendiente"
            evaluacion.aprobado -> "true"
            else -> "false"
        },
        razon = evaluacion.mensaje,
        detalles = evaluacion.detalles?.values?.toList()
    )
}

