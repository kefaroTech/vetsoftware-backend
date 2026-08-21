package com.vetsoftware.app.infrastructure.email;

/**
 * Desenlace real de un envío de correo, tal y como lo conoce el hilo del pool
 * {@code emailTaskExecutor} — que es el <b>único</b> sitio donde se conoce.
 *
 * <p>
 * <b>Por qué existe (issue #242).</b> {@link ResendEmailClient#send} es
 * {@code @Async} y por contrato no lanza: al otro lado del salto de hilo el
 * llamador ya recibió el control, así que cualquier {@code try/catch} suyo
 * alrededor de la llamada es código muerto para el 100 % de los fallos reales
 * (403 de Resend, 429, timeout, dominio no verificado). El llamador que quiera
 * contar entregas —y no encolados— tiene que componer sobre el
 * {@code CompletableFuture} que devuelve el envío y leer este valor.
 *
 * <p>
 * <b>El futuro nunca se completa excepcionalmente.</b> Se mantiene el contrato
 * de «nunca lanza» de la clase: los cuatro flujos que ignoran el retorno siguen
 * siendo fire-and-forget exactos, y quien sí lo compone no necesita
 * {@code exceptionally} para el camino normal.
 */
public enum EmailDispatchOutcome {

    /**
     * Resend aceptó el mensaje (HTTP 2xx). Es lo más cerca de «entregado» que se
     * puede afirmar desde aquí: el rebote posterior en el buzón del destinatario no
     * llega por esta vía.
     */
    ACCEPTED,

    /**
     * No se intentó el envío porque el correo está deshabilitado
     * ({@code vetsoftware.email.enabled=false}), que es el modo normal de dev. No
     * es un fallo y no debe contarse como tal: hacerlo llenaría de falsos positivos
     * cualquier alerta de tasa de error calculada sobre entornos sin correo.
     */
    SKIPPED,

    /**
     * El mensaje no salió y no hay reintento: se perdió. Cubre tanto el rechazo o
     * la caída del proveedor como la configuración incompleta (destinatario vacío,
     * API key ausente) — desde el punto de vista del destinatario los tres casos
     * son el mismo hecho: no le llegó.
     */
    FAILED
}
