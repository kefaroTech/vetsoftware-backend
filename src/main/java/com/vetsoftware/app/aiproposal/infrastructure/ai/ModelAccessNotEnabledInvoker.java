package com.vetsoftware.app.aiproposal.infrastructure.ai;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * El invocador que se despliega hoy: <strong>declara que no hay
 * modelo</strong>.
 *
 * <p>
 * ⛔ <strong>No es un placeholder, es el estado real del sistema.</strong> El
 * acceso al modelo en Bedrock <em>no esta habilitado</em>: depende de un
 * formulario manual en la consola de AWS que el dueno de la cuenta no ha
 * completado (plan S10.1.1). Con este bean instalado la feature responde por el
 * camino determinista, que es una propuesta correcta —el motor de S4.4 no
 * necesita al modelo para cerrar dependencias, meter el nucleo y cotizar—, solo
 * sin la lectura del texto libre.
 *
 * <p>
 * <strong>Por que un bean y no un {@code null} ni una excepcion al
 * arrancar.</strong> Un puerto sin implementacion tumba el contexto de Spring y
 * con el las 93 rodajas de integracion del repositorio; una excepcion al
 * arrancar convierte "una funcionalidad no disponible" en "el backend no
 * levanta". El estado degradado tiene que ser un estado del sistema, no una
 * averia.
 *
 * <p>
 * <strong>Como se sustituye.</strong> El dia que el acceso se habilite y entre
 * la dependencia {@code com.anthropic:anthropic-java}, la implementacion real
 * se declara {@code @Primary} o esta se condiciona a su ausencia. Nada mas del
 * adaptador cambia: el prompt, la validacion, el saneador y el tope de gasto ya
 * estan probados contra esta costura.
 */
@Component
public class ModelAccessNotEnabledInvoker implements ModelInvoker {

    private static final Logger log = LoggerFactory.getLogger(ModelAccessNotEnabledInvoker.class);

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
     * {@code INFO} y no {@code WARN}: es configuracion aplicada, no una anomalia, y
     * es un evento por arranque y no un bucle. Quien vigila que esto no dure meses
     * es la serie {@code ai_proposal_generated_total} con
     * {@code ai_outcome="degraded_model_unavailable"}, no este log.
     */
    @PostConstruct
    void anunciar() {
        log.info("El acceso al modelo de Bedrock no esta habilitado en esta cuenta: el asistente"
                + " comercial servira el 100 % de las propuestas por el camino determinista."
                + " El recuento vive en ai_proposal_generated_total con"
                + " ai_outcome=degraded_model_unavailable");
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
        throw new ModelInvocationException(FAILURE_CODE,
                "el acceso al modelo no esta habilitado en esta cuenta de AWS");
    }
}
