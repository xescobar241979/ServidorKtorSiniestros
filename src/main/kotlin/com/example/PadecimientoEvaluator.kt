package com.redenlace.siniestros.evaluation

import com.redenlace.siniestros.integrations.OpenAiService

class PadecimientoEvaluator(private val openAiService: OpenAiService) {

    suspend fun evaluarPadecimientoConPeriodoDeEspera(padecimiento: String): String {
        val resultado = openAiService.buscarPeriodosDeEspera(padecimiento)

        return if (resultado.periodoEsperaAplica) {
            "Sí, requiere periodo de espera"
        } else {
            "No aplica"
        }
    }
}
