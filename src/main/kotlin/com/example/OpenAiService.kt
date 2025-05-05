package com.redenlace.siniestros.integrations

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class ResultadoOpenAi(
    val periodoEsperaAplica: Boolean,
    val detalles: String
)

class OpenAiService {

    private val ktorClient = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    private val okHttpClient = OkHttpClient()

    private val apiKey = System.getenv("OPENAI_API_KEY")
        ?: throw Exception("❌ No se encontró la API Key de OpenAI.")

    // ✅ Evaluación de padecimientos
    suspend fun buscarPeriodosDeEspera(padecimiento: String): ResultadoOpenAi {
        val url = "https://api.openai.com/v1/chat/completions"

        val requestBody = buildJsonObject {
            put("model", JsonPrimitive("gpt-4"))
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", "Eres un experto en seguros médicos en México. Debes responder estrictamente 'Sí, tiene periodo de espera.' o 'No, no tiene periodo de espera.' sobre un padecimiento.")
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", "¿El padecimiento '$padecimiento' tiene periodo de espera en seguros médicos?")
                })
            })
            put("temperature", JsonPrimitive(0.1))
        }

        val response: HttpResponse = ktorClient.post(url) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        val responseText = response.bodyAsText()

        if (!response.status.isSuccess()) {
            throw Exception("❌ Request fallido: $responseText")
        }

        val jsonResponse = Json.parseToJsonElement(responseText).jsonObject
        val messageContent = jsonResponse["choices"]
            ?.jsonArray?.get(0)?.jsonObject
            ?.get("message")?.jsonObject
            ?.get("content")?.jsonPrimitive?.content
            ?: ""

        val periodoAplica = messageContent.contains("sí", ignoreCase = true)

        return ResultadoOpenAi(
            periodoEsperaAplica = periodoAplica,
            detalles = messageContent
        )
    }
    suspend fun validarFechasSiniestroConOpenAI(fechaAntiguedad: String, fechaSintomas: String): String {
        val prompt = """
        Eres un experto en seguros médicos. Evalúa si estas fechas son razonables:

        - Fecha de antigüedad del certificado: $fechaAntiguedad
        - Fecha de inicio del padecimiento (informe médico): $fechaSintomas

        Responde estrictamente con una de estas opciones:
        ✅ Fechas válidas
        ⚠️ Fechas sospechosas
        ❌ Fechas inconsistentes
    """.trimIndent()

        val requestBody = buildJsonObject {
            put("model", JsonPrimitive("gpt-4"))
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", "Evalúa si las fechas de siniestro son razonables y consistentes.")
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", JsonPrimitive(0.0))
        }

        val response: HttpResponse = ktorClient.post("https://api.openai.com/v1/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        val json = Json.parseToJsonElement(response.bodyAsText())
        return json.jsonObject["choices"]?.jsonArray?.get(0)
            ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
            ?: "Sin respuesta"
    }
    suspend fun extraerNombreDesdeTexto(texto: String): String {
        val prompt = """
        Del siguiente texto de un documento médico o certificado, extrae únicamente el nombre completo del asegurado (persona asegurada).
        Si no lo encuentras, responde con "NO DETECTADO".

        Texto:
        $texto
    """.trimIndent()

        val requestBody = buildJsonObject {
            put("model", JsonPrimitive("gpt-4"))
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", "Eres un asistente experto en procesamiento de documentos médicos.")
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", JsonPrimitive(0.0))
        }

        val response: HttpResponse = ktorClient.post("https://api.openai.com/v1/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        val json = Json.parseToJsonElement(response.bodyAsText())
        return json.jsonObject["choices"]?.jsonArray?.get(0)
            ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
            ?: "NO DETECTADO"
    }


    // ✅ Validación inteligente de nombre asegurado
    suspend fun validarNombreConOpenAI(nombreCertificado: String, nombreDocumento: String): String {
        val prompt = """
            Compara los siguientes nombres de asegurado.

            - Si son el mismo asegurado, aunque tengan abreviaciones, errores menores o diferencias sutiles, responde con exactamente: "Sí, es el mismo asegurado."
            - Si hay diferencias que requieren confirmación manual, responde exactamente: "⚠️ Favor de validar correctamente los nombres, para estar seguros de que es el mismo asegurado."

            Certificado: $nombreCertificado
            Documento: $nombreDocumento
        """.trimIndent()

        val requestBody = buildJsonObject {
            put("model", JsonPrimitive("gpt-4"))
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", "Eres un asistente experto en siniestros médicos. Evalúa si los nombres coinciden, incluso con abreviaciones o errores OCR.")
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", JsonPrimitive(0.0))
        }

        val response: HttpResponse = ktorClient.post("https://api.openai.com/v1/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        val json = Json.parseToJsonElement(response.bodyAsText())
        return json.jsonObject["choices"]?.jsonArray?.get(0)
            ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
            ?: "Sin respuesta"
    }

    // 🔁 Alternativa para usar categorías médicas
    suspend fun buscarPeriodoDeEspera(padecimiento: String): String {
        val prompt = """
            ¿El siguiente padecimiento está relacionado con alguna de estas categorías médicas que requieren periodo de espera en seguros?

            - Cardiopatías
            - Enfermedades respiratorias
            - Cáncer del aparato digestivo
            - Cirugía Bariátrica
            - VIH y SIDA
            - Enfermedades ginecológicas
            - Enfermedades de columna
            - Litiasis renal y urinaria
            - Enfermedades anorrectales
            - Amígdalas y adenoides
            - Enfermedades del piso pélvico
            - Enfermedades acido-pépticas

            Padecimiento: $padecimiento

            Responde únicamente con el nombre de la categoría si aplica, o con "NO APLICA".
        """.trimIndent()

        val requestBody = buildJsonObject {
            put("model", JsonPrimitive("gpt-3.5-turbo"))
            put("messages", JsonArray(listOf(
                buildJsonObject {
                    put("role", JsonPrimitive("system"))
                    put("content", JsonPrimitive("Eres un asistente experto en seguros médicos. Devuelve únicamente el padecimiento relacionado a periodos de espera. Si no aplica, responde 'NO APLICA'."))
                },
                buildJsonObject {
                    put("role", JsonPrimitive("user"))
                    put("content", JsonPrimitive(prompt))
                }
            )))
            put("temperature", JsonPrimitive(0.0))
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string()

        if (!response.isSuccessful) {
            throw Exception("❌ Request fallido: $responseBody")
        }

        val jsonResponse = Json.parseToJsonElement(responseBody!!)
        val content = jsonResponse.jsonObject["choices"]
            ?.jsonArray?.get(0)?.jsonObject?.get("message")
            ?.jsonObject?.get("content")?.jsonPrimitive?.content

        return content ?: "Sin respuesta"
    }
}
