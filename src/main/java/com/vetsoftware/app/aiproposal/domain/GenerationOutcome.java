package com.vetsoftware.app.aiproposal.domain;

/**
 * Como se produjo la propuesta. Es la etiqueta {@code outcome} de la metrica de
 * S9.2, y por eso separa poblaciones que hacia fuera son indistinguibles.
 *
 * <p>
 * ⛔ <strong>Ninguno de estos valores es un 500.</strong> El endpoint responde
 * siempre 200 con la propuesta que se pudo construir: el prospecto no puede
 * hacer nada con un error, y en varios de estos casos ya se pago por la
 * llamada.
 *
 * <p>
 * ⚠️ <strong>Y ninguno sale por HTTP tal cual.</strong> Distinguir hacia fuera
 * "se agoto el presupuesto" de "el modelo fallo" le dice a un observador
 * anonimo <em>cuando</em> se vacio el cupo diario de la plataforma, que es
 * justo lo que necesita saber quien quiera vaciarlo barato.
 */
public enum GenerationOutcome {

    /** El modelo respondio y su salida paso la validacion. */
    SUCCEEDED,

    /**
     * El tope de gasto diario esta agotado. No se invoca: es fail-closed, y el
     * carrito sale del camino determinista.
     */
    DEGRADED_SPEND_CAP,

    /**
     * {@code catalog_item_ai_hints} esta vacio. Es un estado legitimo de una base
     * recien migrada —el changeset 382 no inserta nada si no hay
     * {@code system_users}— y no un error: sin hints no hay prompt que enviar y no
     * se inventa uno.
     */
    DEGRADED_NO_HINTS,

    /**
     * No hay acceso al modelo. Hoy es el estado permanente: el formulario de
     * habilitacion de Bedrock esta sin completar (plan S10.1.1).
     */
    DEGRADED_MODEL_UNAVAILABLE,

    /**
     * Se invoco, se pago y no sirvio: excepcion, timeout, o una salida que no paso
     * la validacion. Poblacion distinta de las degradaciones, porque aqui si hubo
     * coste.
     */
    MODEL_FAILED;

    // ⛔ Aqui vivia esDegradacionSinLlamada(): las tres degradaciones que no
    // llegaron a llamar al modelo y por tanto responden en milisegundos. Su unico
    // consumidor era el suelo de latencia de S4.2.3, que se retiro porque el bit
    // que ocultaba -"esto salio degradado"- lo publica la respuesta en el campo
    // `presentation`. El argumento completo, y por que reintroducirlo seria un
    // error, esta escrito en ProposalAssembler.presentacion. Si vuelve a hacer
    // falta el predicado, se reescribe alli mismo: en este enum era codigo muerto.
}
