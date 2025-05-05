package com.example

import com.redenlace.siniestros.integrations.OpenAiService
import io.ktor.client.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.config.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths



fun main(args: Array<String>) = EngineMain.main(args)

fun configureGoogleCredentials() {
    val path = Paths.get("src/main/resources/keys/adept-rock-455216-q9-92e7410df42a.json").toAbsolutePath().toString()
    val file = File(path)
    if (!file.exists()) {
        println("❌ Archivo de credenciales de Google no encontrado en: $path")
        return
    }

    System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", path)
    println("✅ Credenciales de Google configuradas correctamente en: $path")
}

@Suppress("unused")
fun Application.module() {
    val logger = LoggerFactory.getLogger("ServidorKtorSiniestros")

    configureGoogleCredentials()

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(CallLogging)

    install(CORS) {
        anyHost()
        allowNonSimpleContentTypes = true
    }

    // 🔐 Cargar la API key desde application.conf
    val openAiService = OpenAiService() // ✅ Correcta para tu clase actual



    logger.info("🟢 Iniciando servidor Ktor...")
    configureRouting(openAiService)
}
