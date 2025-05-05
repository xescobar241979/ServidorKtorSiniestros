package com.redenlace.siniestros.model

import kotlinx.serialization.Serializable

@Serializable
data class SolicitudEvaluacion(
    val descripcionPadecimiento: String
)
