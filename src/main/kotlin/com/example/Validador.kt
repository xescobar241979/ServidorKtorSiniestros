package com.example

import kotlinx.serialization.json.*
import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// 🔠 Normaliza texto para comparar
fun normalizarTexto(texto: String): String {
    return texto
        .lowercase()
        .replace(Regex("[^a-záéíóúñ ]"), "")
        .replace("\\s+".toRegex(), " ")
        .trim()
}


// 📌 Diccionario de palabras clave por tipo de documento
val palabrasClavePorDocumento = mapOf(
    "Solicitud de reclamación" to listOf("solicitud", "formato reembolso", "reclamacion", "red enlace"),
    "Identificación oficial" to listOf("credencial", "ine", "instituto nacional electoral", "id oficial"),
    "Informe Médico" to listOf("informe médico", "diagnóstico", "tratante"),
    "Finiquito" to listOf("finiquito", "carta finiquito", "autorizacion de pago"),
    "Corte de caja" to listOf("corte de caja", "Corte", "de", "caja", "resumen de gastos"),
    "Estado de cuenta" to listOf("estado de cuenta", "banco", "movimientos"),
    "Certificado" to listOf("certificado", "cliente titular", "adicionales")
)
fun extraerNombresDelCertificado(texto: String): List<String> {
    val nombres = mutableListOf<String>()

    // Buscar cliente titular
    val regexTitular = Regex("""cliente titular\s*[:\-]?\s*([a-záéíóúñ ]{5,})""", RegexOption.IGNORE_CASE)
    regexTitular.findAll(texto).forEach { match ->
        nombres.add(match.groupValues[1].trim())
    }

    // Buscar nombres en la sección "Adicionales"
    val regexAdicional = Regex("""\d+\s+([a-záéíóúñ ]{5,})""", RegexOption.IGNORE_CASE)
    regexAdicional.findAll(texto).forEach { match ->
        val nombre = match.groupValues[1].trim()
        // Evitar agregar cosas como "Masculino", "Femenino", etc.
        if (!nombre.contains("masculino", true) && !nombre.contains("femenino", true)) {
            nombres.add(nombre)
        }
    }

    return nombres.distinct()
}

fun nombresCoinciden(nombre1: String, nombre2: String): Boolean {
    val normalizado1 = normalizarTexto(nombre1).split(" ").toSet()
    val normalizado2 = normalizarTexto(nombre2).split(" ").toSet()
    val interseccion = normalizado1.intersect(normalizado2)

    // Requiere al menos 2 palabras en común para decir que coincide
    return interseccion.size >= 2
}



// 🧠 Extrae nombres del certificado
fun extraerNombreDesdeFiniquito(texto: String): String? {
    val lineas = texto.lines().map { it.trim() }
    val indice = lineas.indexOfFirst { it.lowercase().contains("nombre del paciente") }

    if (indice != -1) {
        val posiblesNombres = mutableListOf<String>()

        // Buscar hacia adelante solo líneas válidas (evita saltar por líneas vacías o numéricas)
        for (i in (indice + 1)..(indice + 4).coerceAtMost(lineas.lastIndex)) {
            val linea = lineas[i]
            if (linea.isNotBlank() && linea.length > 3 && !linea.any { it.isDigit() }) {
                posiblesNombres.add(linea)
            } else {
                break
            }
        }

        val nombre = posiblesNombres.joinToString(" ")
        if (nombre.isNotBlank()) {
            return normalizarTexto(nombre)
        }
    }

    return null
}


// 📅 Extrae antigüedad buscando alrededor del nombre
fun extraerAntiguedadDesdeCertificado(texto: String): String? {
    val regex = Regex("""antig(u|ü)edad.*?(\d{2}/\d{2}/\d{4})""", RegexOption.IGNORE_CASE)
    return regex.find(texto)?.groupValues?.getOrNull(2)
}

// 📅 Convierte fecha de diferentes formatos
fun formatearFecha(fecha: String): String? {
    return try {
        when {
            fecha.matches(Regex("\\d{8}")) ->
                LocalDate.parse(fecha, DateTimeFormatter.ofPattern("yyyyMMdd")).toString()
            fecha.matches(Regex("\\d{2}/\\d{2}/\\d{4}")) ->
                LocalDate.parse(fecha, DateTimeFormatter.ofPattern("dd/MM/yyyy")).toString()
            fecha.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> fecha
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

// 📄 Genera reporte a partir del texto extraído
fun generarReporteDesdeTextos(textos: JsonObject): JsonObject {
    // 1. Extraer texto de documentos clave
    val textoCertificado = textos.entries.firstOrNull {
        it.key.contains("certificado", ignoreCase = true)
    }?.value?.jsonPrimitive?.contentOrNull ?: ""

    val textoFiniquito = textos.entries.firstOrNull {
        it.key.contains("finiquito", ignoreCase = true)
    }?.value?.jsonPrimitive?.contentOrNull ?: ""

    val textoInformeMedico = textos.entries.firstOrNull {
        it.key.contains("informe", ignoreCase = true)
    }?.value?.jsonPrimitive?.contentOrNull ?: ""

    // 2. Extraer nombre del paciente preferiblemente del informe médico o finiquito
    val regexNombrePaciente = Regex("""nombre del paciente\s*[:\-]?\s*([a-záéíóúñ ]{5,})""", RegexOption.IGNORE_CASE)
    val nombreDesdeInforme = regexNombrePaciente.find(textoInformeMedico)?.groupValues?.getOrNull(1)?.trim()
    val nombreDesdeFiniquito = regexNombrePaciente.find(textoFiniquito)?.groupValues?.getOrNull(1)?.trim()

    val nombrePaciente = extraerNombreDesdeFiniquito(textoFiniquito)
        ?: extraerNombreDesdeFiniquito(textoInformeMedico)
        ?: nombreDesdeFiniquito
        ?: nombreDesdeInforme
        ?: "desconocido"

    println("📆 Nombre paciente: $nombrePaciente")

    // 3. Extraer nombres del certificado
    val nombresCertificado = extraerNombresDelCertificado(textoCertificado)
    println("📄 Nombres extraídos del certificado: ${nombresCertificado.joinToString()}")

    // 4. Verificar si el nombre coincide
    val nombreValido = nombresCertificado.any { nombresCoinciden(it, nombrePaciente) }
    println("✅ ¿Nombre del paciente coincide con el certificado? $nombreValido")

    // 5. Extraer y formatear la fecha de antigüedad
    val fechaAntiguedad = extraerAntiguedadDesdeCertificado(textoCertificado)
    val fechaFormateada = formatearFecha(fechaAntiguedad ?: "2023-01-01")
    println("📅 Fecha de antigüedad extraída del certificado: $fechaFormateada")

    // 6. Construcción del JSON de respuesta
    return buildJsonObject {
        put("documentosAdjuntos", JsonArray(textos.keys.map { JsonPrimitive(it) }))
        put("fechaContratacion", JsonPrimitive(fechaFormateada))
        put("fechaInicioSintomas", JsonPrimitive("2025-03-21")) // automatizar si es posible
        put("deducible", JsonPrimitive(10000))
        put("montoDeduciblePagado", JsonPrimitive(10000))
        put("clienteEnRedEnlace", JsonPrimitive(true))
        put("enfermedadPosiblePreexistente", JsonPrimitive(false))
        put("paciente", JsonPrimitive(nombrePaciente))
        put("nombreValido", JsonPrimitive(nombreValido))
    }
}


// ✅ Valida todas las reglas
fun evaluarReglas(
    reglas: List<ReglaNegocio>,
    reporte: JsonObject,
    textos: JsonObject
): List<EvaluacionResultado> {
    val resultados = mutableListOf<EvaluacionResultado>()

    val nombrePaciente = reporte["paciente"]?.jsonPrimitive?.contentOrNull ?: "desconocido"
    val fechaContratacion = reporte["fechaContratacion"]?.jsonPrimitive?.contentOrNull
    val fechaSintomas = reporte["fechaInicioSintomas"]?.jsonPrimitive?.contentOrNull
    val deducible = reporte["deducible"]?.jsonPrimitive?.intOrNull ?: 0
    val deduciblePagado = reporte["montoDeduciblePagado"]?.jsonPrimitive?.intOrNull ?: 0
    val documentos = reporte["documentosAdjuntos"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

    // 📌 Validación 1: Nombre
    val textoCertificado = textos.entries.firstOrNull {
        it.key.contains("certificado", ignoreCase = true)
    }?.value?.jsonPrimitive?.contentOrNull ?: ""

    val nombresCertificado = extraerNombresDelCertificado(textoCertificado)
    val nombreValido = nombresCertificado.any { nombresCoinciden(it, nombrePaciente) }

    if (nombreValido) {
        resultados.add(
            EvaluacionResultado(
                regla = "nombreCoincide",
                estado = "aprobado",
                razon = "✅ Nombre coincide con certificado"
            )
        )
    } else {
        resultados.add(
            EvaluacionResultado(
                regla = "nombreCoincide",
                estado = "rechazado",
                razon = "❌ El nombre del asegurado no coincide con el certificado:\n→ Certificado: ${nombresCertificado.joinToString()}\n→ Documento: $nombrePaciente"
            )
        )
    }

    // 📌 Validación 2: Fecha de síntomas después de contratación
    if (fechaContratacion != null && fechaSintomas != null) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val fecha1 = LocalDate.parse(fechaContratacion, formatter)
        val fecha2 = LocalDate.parse(fechaSintomas, formatter)

        if (fecha2.isBefore(fecha1)) {
            resultados.add(
                EvaluacionResultado(
                    regla = "fechaSintomasPosteriorContratacion",
                    estado = "rechazado",
                    razon = "❌ Regla no cumplida: La fecha de síntomas no puede ser antes de la contratación"
                )
            )
        } else {
            resultados.add(
                EvaluacionResultado(
                    regla = "fechaSintomasPosteriorContratacion",
                    estado = "aprobado",
                    razon = "✅ La fecha de síntomas es válida"
                )
            )
        }
    }

    // 📌 Validación 3: Deducible pagado suficiente
    if (deduciblePagado >= deducible) {
        resultados.add(
            EvaluacionResultado(
                regla = "deducibleCubierto",
                estado = "aprobado",
                razon = "✅ El deducible pagado alcanza el deducible contratado"
            )
        )
    } else {
        resultados.add(
            EvaluacionResultado(
                regla = "deducibleCubierto",
                estado = "pendiente",
                razon = "⚠️ El deducible pagado es menor al requerido"
            )
        )
    }

    // 📌 Validación 4: Documentos requeridos
    val documentosRequeridos = listOf(
        "certificado", "finiquito", "informe", "solicitud", "corte de caja", "estado de cuenta"
    )

    val documentosFaltantes = documentosRequeridos.filter { requerido ->
        documentos.none { it.contains(requerido, ignoreCase = true) }
    }

    if (documentosFaltantes.isEmpty()) {
        resultados.add(
            EvaluacionResultado(
                regla = "documentosAdjuntos",
                estado = "aprobado",
                razon = "✅ Todos los documentos requeridos están presentes"
            )
        )
    } else {
        resultados.add(
            EvaluacionResultado(
                regla = "documentosAdjuntos",
                estado = "pendiente",
                razon = "⚠️ Documentos clave no adjuntados: ${documentosFaltantes.joinToString()}"
            )
        )
    }

    return resultados
}

