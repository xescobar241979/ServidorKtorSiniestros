package com.example

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.http.content.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.File
import io.ktor.server.plugins.*

fun Application.configureRouting() {
    routing {
        // Evaluar siniestro con JSON de ejemplo
        get("/evaluar-siniestro") {
            try {
                val resource = this::class.java.classLoader.getResource("reporte_sini.json")
                if (resource == null) {
                    call.respond(HttpStatusCode.NotFound, "Archivo JSON no encontrado")
                    return@get
                }

                val jsonString = File(resource.toURI()).readText()
                val reporte = Json.decodeFromString<ReporteMedico>(jsonString)
                val resultado = evaluarSiniestro(reporte)

                call.respond(resultado)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ResultadoEvaluacion(false, "Error al procesar: ${e.message}")
                )
            }
        }

        // 🆕 Subir múltiples archivos
        post("/upload") {
            val multipart = call.receiveMultipart()
            var siniestroId = ""

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        if (part.name == "siniestroId") {
                            siniestroId = part.value
                        }
                    }
                    is PartData.FileItem -> {
                        val fileName = part.originalFileName ?: "archivo"
                        val folder = File("uploads/$siniestroId")
                        folder.mkdirs()
                        val file = File(folder, fileName)
                        part.streamProvider().use { input ->
                            file.outputStream().buffered().use { output ->
                                output.write(input.readBytes())
                            }
                        }
                    }
                    else -> {}
                }
                part.dispose()
            }

            // ✅ Respuesta al cliente
            call.respond(HttpStatusCode.OK, "✅ Archivos subidos correctamente para siniestro: $siniestroId")
        }

        // 🧾 Archivos estáticos: permite acceder a upload.html, imágenes, etc.
        staticResources("/static", "static")

        // 🏠 Ruta raíz: redirige automáticamente a upload.html
        get("/") {
            call.respondRedirect("/static/upload.html")
        }
    }
}
