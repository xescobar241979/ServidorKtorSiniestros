package com.example

import com.redenlace.siniestros.integrations.OpenAiService
import com.redenlace.siniestros.model.SolicitudEvaluacion
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.http.content.*
import kotlinx.serialization.Serializable
import java.io.File
import org.slf4j.LoggerFactory
import kotlinx.serialization.json.jsonPrimitive


@Serializable
data class ResultadoOCR(
    val procede: String,
    val razon: String,
    val detalles: List<String>? = null
)

@Serializable
data class RespuestaSiniestro(
    val procedente: Boolean,
    val razon: String
)

fun Application.configureRouting(openAiService: OpenAiService) {
    val logger = LoggerFactory.getLogger("ServidorKtorSiniestros")

    routing {

        post("/evaluar-siniestro") {
            try {
                val solicitud = call.receive<SolicitudEvaluacion>()
                val resultado = openAiService.buscarPeriodosDeEspera(solicitud.descripcionPadecimiento)

                val respuesta = if (resultado.periodoEsperaAplica) {
                    RespuestaSiniestro(false, "Requiere periodo de espera. Detalles: ${resultado.detalles}")
                } else {
                    RespuestaSiniestro(true, "No requiere periodo de espera.")
                }

                call.respond(HttpStatusCode.OK, respuesta)

            } catch (e: Exception) {
                logger.error("❌ Error evaluando siniestro", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "❌ Error evaluando siniestro: ${e.message}")
                )
            }
        }

        post("/upload") {
            try {
                val multipart = call.receiveMultipart()
                val archivos = mutableListOf<File>()

                multipart.forEachPart { part ->
                    if (part is PartData.FileItem) {
                        val nombreArchivo = part.originalFileName ?: "archivo.pdf"
                        val archivo = File("uploads/$nombreArchivo")
                        part.streamProvider().use { input ->
                            archivo.outputStream().use { output -> input.copyTo(output) }
                        }
                        archivos.add(archivo)
                    }
                    part.dispose()
                }

                if (archivos.isEmpty()) {
                    logger.warn("⚠️ No se recibió ningún archivo")
                    call.respond(HttpStatusCode.BadRequest, ResultadoOCR("false", "❌ No se recibió ningún archivo."))
                    return@post
                }

                logger.info("📂 Archivos recibidos: ${archivos.joinToString { it.name }}")

                logger.info("🧪 Iniciando procesamiento con Document AI...")
                val textosExtraidos = procesarDocumentosConDocumentAI(archivos)
                logger.info("✅ Documentos procesados con Document AI")


                // 🔍 Extraer y validar nombres usando OpenAI
                val textoCertificado = textosExtraidos["Certificado - DV3523.pdf"]?.jsonPrimitive?.content ?: ""
                val textoInforme = textosExtraidos["Informe Medico.pdf"]?.jsonPrimitive?.content ?: ""

                val nombreCertificado = openAiService.extraerNombreDesdeTexto(textoCertificado)
                val nombreDocumento = openAiService.extraerNombreDesdeTexto(textoInforme)

                val resultadoNombre = openAiService.validarNombreConOpenAI(nombreCertificado, nombreDocumento)


                // 🔁 Continuamos con evaluación de reglas
                val reporte = generarReporteDesdeTextos(textosExtraidos)
                val reglas = cargarReglasDesdeJson()
                val evaluacion = evaluarReglas(reglas, reporte, textosExtraidos)

                val rechazadas = evaluacion.filter { it.estado == "rechazado" }
                val pendientes = evaluacion.filter { it.estado == "pendiente" }

                val estadoFinal = when {
                    resultadoNombre.contains("⚠️") -> "pendiente"
                    resultadoNombre.contains("sí", ignoreCase = true) && rechazadas.isEmpty() -> "true"
                    else -> "false"
                }

                val razonFinal = when {
                    resultadoNombre.contains("⚠️") -> resultadoNombre
                    !resultadoNombre.contains("sí", ignoreCase = true) -> resultadoNombre
                    rechazadas.isNotEmpty() -> rechazadas.first().razon
                    pendientes.isNotEmpty() -> pendientes.first().razon
                    else -> "✅ Siniestro aprobado"
                }

                call.respond(
                    HttpStatusCode.OK,
                    ResultadoOCR(
                        procede = estadoFinal,
                        razon = razonFinal,
                        detalles = listOf(
                            "Certificado: $nombreCertificado",
                            "Documento: $nombreDocumento"
                        ) + evaluacion.map { it.razon }
                    )
                )

            } catch (e: Exception) {
                logger.error("❌ Error procesando archivos", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ResultadoOCR("false", "❌ Error interno del servidor: ${e.message}")
                )
            }
        }

        // Archivos estáticos
        staticResources("/static", "static")

        get("/") {
            call.respondRedirect("/static/upload.html")
        }
    }
}
