package com.example

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Paths

fun cargarReglasDesdeJson(): List<ReglaNegocio> {
    val path = Paths.get("src/main/resources/static/reglas.negocio.json")
    val jsonContent = Files.readString(path)
    return Json.decodeFromString(jsonContent)
}
