package com.vetsoftware.app.aiproposal.infrastructure.ai;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * El invocador que se cablea cuando <strong>la invocacion del modelo esta
 * apagada por configuracion</strong>: declara que no hay modelo y no llama a
 * nadie.
 *
 * <p>
 * ⛔ <strong>Esta clase NO consulta a AWS, y su nombre y su mensaje lo dicen
 * ahora a proposito.</strong> Se llamaba {@code ModelAccessNotEnabledInvoker} y
 * anunciaba al arrancar que «el acceso al modelo de Bedrock no esta habilitado
 * en esta cuenta». Eso es un hecho sobre la cuenta de AWS que aqui no se
 * comprueba de ninguna forma: lo unico que decide si este bean existe es la
 * negacion de {@link BedrockInvokerConfig#ACTIVO}, es decir que la propiedad
 * {@code vetsoftware.ai.proposal.bedrock.enabled} no valga {@code true}.
 *
 * <p>
 * <strong>Y el mensaje llego a ser falso.</strong> En la cuenta de dev el
 * acceso al modelo estaba {@code AUTHORIZED} y {@code AVAILABLE} y tres tareas
 * consecutivas escribieron lo contrario; la causa real era que la variable
 * {@code AI_PROPOSAL_BEDROCK_ENABLED} <em>no existia</em> en la definicion de
 * tarea. Una linea de arranque que culpa a un tercero de algo que nunca miro
 * manda el diagnostico a la consola de AWS —a pedir permisos que ya estaban
 * concedidos— en vez de a la variable que falta. Por eso el texto nombra ahora
 * la propiedad <em>y</em> la variable de entorno: lo que la clase sabe, y donde
 * mirar.
 *
 * <p>
 * <strong>La comprobacion real contra Bedrock se descarto</strong>, no se
 * olvido: exigiria un permiso IAM nuevo solo para poder escribir un log. Lo que
 * si tiene arreglo barato es no afirmar lo que no se sabe.
 *
 * <p>
 * <strong>Por que un bean y no un {@code null} ni una excepcion al
 * arrancar.</strong> Un puerto sin implementacion tumba el contexto de Spring y
 * con el las 93 rodajas de integracion del repositorio; una excepcion al
 * arrancar convierte "una funcionalidad no disponible" en "el backend no
 * levanta". El estado degradado tiene que ser un estado del sistema, no una
 * averia. Con este bean instalado la feature responde por el camino
 * determinista, que es una propuesta correcta —el motor de S4.4 no necesita al
 * modelo para cerrar dependencias, meter el nucleo y cotizar—, solo sin la
 * lectura del texto libre.
 *
 * <p>
 * <strong>Como se sustituye. Ya paso, y por las dos vias a la vez.</strong> La
 * implementacion real es {@link BedrockModelInvoker} —por Bedrock y no por
 * {@code com.anthropic:anthropic-java}, porque el contenedor se autentica con
 * el rol de tarea de ECS y lo que la infraestructura concede es un ARN de
 * Bedrock—, se declara {@code @Primary} <em>y</em> esta se condiciona a su
 * ausencia. Nada mas del adaptador cambio: el prompt, la validacion, el
 * saneador y el tope de gasto ya estaban probados contra esta costura.
 *
 * <p>
 * ⛔ <strong>La condicion es la negacion exacta de
 * {@link BedrockInvokerConfig#ACTIVO}, y comparte su constante a
 * proposito.</strong> Este bean tiene que existir siempre que no exista el
 * otro: si las dos condiciones fueran falsas a la vez no habria ningun
 * {@link ModelInvoker}, el contexto no levantaria y se caerian las 93 rodajas
 * de integracion —el desastre entero que esta clase existe para evitar—.
 * Compartir el literal es lo que impide que las dos expresiones se separen en
 * una edicion descuidada.
 *
 * <p>
 * <strong>Y por eso mismo se condiciona en vez de dejarse suelta con un
 * {@code @Primary} delante.</strong> Con Bedrock cableado, este bean seguiria
 * anunciando al arrancar que la invocacion esta apagada sin ser ya verdad; una
 * linea de arranque falsa es peor que ninguna, porque es la primera que se lee
 * a las tres de la manana. Que es, otra vez, el motivo de todo este fichero.
 */
@Component
@ConditionalOnExpression("!(" + BedrockInvokerConfig.ACTIVO + ")")
public class BedrockDisabledInvoker implements ModelInvoker {

    private static final Logger log = LoggerFactory.getLogger(BedrockDisabledInvoker.class);

    /**
     * ⛔ <strong>El literal NO se renombro con la clase, y es deliberado.</strong>
     * {@code MODEL_ACCESS_NOT_ENABLED} es vocabulario cerrado: lo declara
     * {@link AiErrorType}, viaja como {@code ai.failure.code} a la telemetria y
     * queda escrito en {@code ai_proposal_turns}. Cambiar la cadena partiria en dos
     * cualquier serie historica y obligaria a una migracion de datos para arreglar
     * la redaccion de un log. La imprecision que quedaba —hablar de "acceso" cuando
     * lo que hay es un interruptor apagado— vive ahora solo en el nombre del
     * codigo, no en el texto que alguien lee a las tres de la manana.
     */
    public static final String FAILURE_CODE = "MODEL_ACCESS_NOT_ENABLED";

    /**
     * <strong>Un estado degradado que no se anuncia es indistinguible de una
     * averia.</strong> Con este bean instalado, el 100 % de las propuestas sale por
     * el camino determinista, y la unica evidencia que tendria un operador seria un
     * contador que no sube y un panel que parece vacio por falta de trafico. Una
     * linea en el arranque cuesta cero y es lo primero que se encuentra a las tres
     * de la manana.
     *
     * <p>
     * ⛔ <strong>El mensaje dice lo que esta clase SABE y nada mas.</strong> Sabe
     * que la propiedad no vale {@code true}; no sabe —ni pregunta— si el acceso al
     * modelo esta concedido en la cuenta. Por eso nombra la propiedad y la variable
     * de entorno que la publica: son los dos sitios donde esta la respuesta, y el
     * fallo real que se vio fue justamente la variable ausente en la definicion de
     * tarea con el acceso ya concedido.
     *
     * <p>
     * {@code INFO} y no {@code WARN}: es configuracion aplicada, no una anomalia, y
     * es un evento por arranque y no un bucle. Quien vigila que esto no dure meses
     * es la serie {@code ai_proposal_generated_total} con
     * {@code ai_outcome="degraded_model_unavailable"}, no este log.
     */
    @PostConstruct
    void anunciar() {
        log.info("La invocacion del modelo esta apagada por configuracion:"
                + " vetsoftware.ai.proposal.bedrock.enabled no vale 'true' (la publica la"
                + " variable de entorno AI_PROPOSAL_BEDROCK_ENABLED). El asistente comercial"
                + " servira el 100 % de las propuestas por el camino determinista. OJO: esto"
                + " NO dice nada sobre el acceso al modelo en la cuenta de AWS, que este"
                + " proceso no consulta; si el acceso ya estuviera concedido, lo que falta es"
                + " la variable en la definicion de tarea. El recuento vive en"
                + " ai_proposal_generated_total con ai_outcome=degraded_model_unavailable");
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    /**
     * Existe para que la costura sea total: si alguien invocara sin preguntar por
     * {@link #isAvailable()}, el resultado es un fallo declarado y no una llamada
     * de red que no se puede hacer.
     */
    @Override
    public ModelInvocation invoke(ProposalPrompt prompt) {
        throw new ModelInvocationException(FAILURE_CODE, "la invocacion del modelo esta apagada:"
                + " vetsoftware.ai.proposal.bedrock.enabled no vale 'true'");
    }
}
