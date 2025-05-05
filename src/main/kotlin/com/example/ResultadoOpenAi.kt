package com.example

import kotlinx.serialization.Serializable

@Serializable
data class ResultadoOpenAi(
    val periodoEsperaAplica: Boolean,
    val detalles: String
)
