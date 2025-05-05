package com.example

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File

@Serializable
data class ExtraccionDocumento(
    val nombre: String,
    val campos: List<String>
)

fun cargarConfiguracionExtraccion(): Map<String, List<String>> {
    val archivo = File("src/main/resources/extraccion_documentos.json")
    val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    val raw = archivo.readText()
    return json.decodeFromString(
        MapSerializer(String.serializer(), ListSerializer(String.serializer())),
        raw
    )
}
