package com.example

import com.google.cloud.documentai.v1.*
import com.google.protobuf.ByteString
import kotlinx.serialization.json.*
import java.io.File

fun procesarDocumentosConDocumentAI(archivos: List<File>): JsonObject {
    val projectId = "909191661840"
    val location = "us"
    val processorId = "90a891c14300a41d"
    val name = ProcessorName.of(projectId, location, processorId).toString()

    val client = DocumentProcessorServiceClient.create()

    var nombreCertificado: String? = null
    var nombreDocumento: String? = null
    val resultados = mutableMapOf<String, String>()

    archivos.forEach { archivo ->
        try {
            val inputBytes = archivo.readBytes()
            val rawDocument = RawDocument.newBuilder()
                .setContent(ByteString.copyFrom(inputBytes))
                .setMimeType("application/pdf")
                .build()

            val request = ProcessRequest.newBuilder()
                .setName(name)
                .setRawDocument(rawDocument)
                .build()

            println("📡 Enviando a Document AI: ${archivo.name}")
            val response = client.processDocument(request)
            val document = response.document
            val texto = document.text

            // Mostrar parte del texto extraído para depuración
            println("📄 Texto extraído de ${archivo.name}:\n${texto.take(500)}\n")

            if (texto.isBlank()) {
                println("⚠️ Documento sin texto: ${archivo.name}")
                resultados[archivo.name] = "⚠️ No se pudo extraer texto del archivo"
                return@forEach
            }

            resultados[archivo.name] = texto

            // 🧠 Intento de extracción de nombre asegurado
            val nombreExtraido = Regex("(?i)(nombre(?: del)?(?: asegurado| paciente)?)[^\n:]*[:\\s]+([A-ZÁÉÍÓÚÑ ]{5,})")
                .find(texto)?.groupValues?.getOrNull(2)?.trim()

            when {
                archivo.name.contains("certificado", ignoreCase = true) -> {
                    nombreCertificado = nombreExtraido ?: "desconocido"
                    println("🧾 Nombre extraído del certificado: $nombreCertificado")
                }
                archivo.name.contains("informe", ignoreCase = true) -> {
                    nombreDocumento = nombreExtraido ?: "desconocido"
                    println("🧾 Nombre extraído del informe médico: $nombreDocumento")
                }
            }

        } catch (e: Exception) {
            println("❌ Error procesando ${archivo.name}: ${e.message}")
            resultados[archivo.name] = "❌ Error procesando archivo: ${e.message}"
        }
    }

    client.shutdown()

    return buildJsonObject {
        resultados.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
        put("nombreCertificado", JsonPrimitive(nombreCertificado ?: "desconocido"))
        put("nombreDocumento", JsonPrimitive(nombreDocumento ?: "desconocido"))
    }
}
