package com.vetsoftware.app.aiproposal.application.port.out;

import com.vetsoftware.app.aiproposal.application.dto.ProposalGenerationRequest;
import com.vetsoftware.app.aiproposal.application.dto.ProposalGenerationResult;

/**
 * La frontera con el modelo. Todo lo que hay detras —el SDK, el prompt, los
 * tokens, el dinero— vive en {@code infrastructure/ai} y no se filtra por aqui.
 *
 * <p>
 * ⛔ <strong>SE INVOCA FUERA DE TODA TRANSACCION.</strong> No es una preferencia
 * de estilo: {@code SIN_IO_EXTERNO_EN_TRANSACCION} se ensancho esta semana
 * justo para esto y hoy veta <em>paquetes enteros por prefijo</em> declarados
 * como cadena —{@code com.anthropic.},
 * {@code software.amazon.awssdk.services.bedrockruntime.},
 * {@code org.springframework.ai.} y cuatro mas—, precisamente porque una regla
 * escrita con literales {@code Class<?>} no podia nombrar un SDK que todavia no
 * era dependencia. Sigue la cadena de llamadas completa, asi que esconder la
 * invocacion tres metodos mas abajo no la despista. Un {@code @Transactional}
 * sobre el caso de uso que llame a este puerto <strong>rompe el build</strong>,
 * y hace bien: una llamada de 3-8 segundos retiene una conexion de Hikari y sus
 * locks durante todo ese tiempo, y con trafico eso tumba el backend entero.
 *
 * <p>
 * <strong>La secuencia obligada</strong> es la que impone
 * {@link com.vetsoftware.app.aiproposal.domain.TurnStatus}: TX1 escribe el
 * turno {@code PENDING} y commitea → se invoca este puerto fuera de transaccion
 * → TX2 lo cierra a {@code SUCCEEDED} o {@code FAILED}.
 *
 * <p>
 * <strong>Nunca lanza por un fallo del modelo.</strong> Un timeout, una salida
 * ilegible o el tope de gasto agotado son resultados, no excepciones: el
 * endpoint responde 200 con el modo degradado en los tres casos, porque el
 * prospecto no puede hacer nada con un error y en dos de ellos ya se pago.
 */
public interface ProposalGeneratorPort {

    ProposalGenerationResult generate(ProposalGenerationRequest request);
}
