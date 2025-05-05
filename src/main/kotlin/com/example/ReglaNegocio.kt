package com.example

import kotlinx.serialization.Serializable

@Serializable
data class ReglaNegocio(
    val nombre: String,
    val descripcion: String,
    val condicion: Condicion,
    val resultado: Resultado
)

@Serializable
data class Condicion(
    val campo1: String? = null,
    val operador: String? = null,
    val campo2: String? = null,
    val tipo: String? = null,
    val requeridos: List<String>? = null,

    // ⚠️ Este campo no se serializa desde JSON, se usa internamente en ejecución
    @kotlinx.serialization.Transient
    val textos: Map<String, String>? = null
)

@Serializable
data class Resultado(
    val estado: String, // "aprobado", "rechazado" o "pendiente"
    val razon: String
)

@Serializable
data class EvaluacionResultado(
    val regla: String,
    val estado: String,
    val razon: String
)
