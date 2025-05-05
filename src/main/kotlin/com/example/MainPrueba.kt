import com.redenlace.siniestros.integrations.OpenAiService
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        val pregunta = "Sangrado uterino anormal secundario a pólipo endometrial"

        // Crear la instancia del servicio
        val openAiService = OpenAiService()

        // Llamar a la función correcta dentro de la clase
        val resultado = openAiService.buscarPeriodosDeEspera(pregunta)

        println("📋 Respuesta de OpenAI: ${resultado.detalles}")
    }
}
