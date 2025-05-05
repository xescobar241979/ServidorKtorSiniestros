package com.example

import com.google.cloud.documentai.v1.*
import com.google.cloud.documentai.v1.Document.Page
import java.io.FileInputStream
import java.nio.file.Paths

fun probarConexionDocumentAI() {
    try {
        val projectId = "adept-rock-455216" // 🔵 Tu ID de proyecto
        val location = "us"                 // 🔵 Ubicación
        val processorId = "90a891c14300a41d"

        val client = DocumentProcessorServiceClient.create()
        val name = ProcessorName.of(projectId, location, processorId).toString()

        println("🔵 Conectado correctamente a Document AI: $name")

        client.shutdown()
        println("✅ Cliente Document AI cerrado exitosamente.")

    } catch (e: Exception) {
        println("❌ Error conectando con Document AI:")
        e.printStackTrace()
    }
}
