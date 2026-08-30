package com.vetsoftware.app.aiproposal.application.port.out;

/**
 * El suelo de latencia de la ruta degradada (plan S4.2.3).
 *
 * <p>
 * &#9940; <strong>El problema, medido:</strong> una generacion real tarda 3-8
 * segundos; una respuesta degradada -tope de gasto agotado, palanca apagada,
 * Valkey caido- vuelve en milisegundos. Esa diferencia de dos ordenes de
 * magnitud le dice a un observador anonimo <strong>con un reloj y nada
 * mas</strong> cuando se agoto el presupuesto diario de la plataforma, y por
 * tanto cuando la competencia puede vaciarlo barato o cuando dejar de
 * intentarlo. Ninguna credencial hace falta para leerlo.
 *
 * <p>
 * <strong>No iguala la distribucion -nada lo hace- pero borra la separacion que
 * se lee a simple vista.</strong> La metrica {@code outcome} sigue
 * distinguiendo las poblaciones hacia dentro; lo que deja de distinguirse es
 * hacia fuera.
 *
 * <p>
 * &#9940; <strong>Solo la degradacion sin llamada.</strong> Un fallo del modelo
 * -timeout, salida ilegible- ya pago la espera, y ponerle un suelo encima seria
 * castigar al prospecto por una averia nuestra.
 *
 * <p>
 * <strong>Es un puerto y no una utilidad</strong> porque un caso de uso que
 * duerme de verdad convierte cada test unitario en tres segundos de espera: el
 * doble lo anula, y la implementacion real se prueba aparte.
 */
public interface ResponsePacingPort {

    /**
     * Espera lo que falte hasta un suelo aleatorio en el rango de S4.2.3.
     *
     * @param elapsedMillis
     *            lo que ya se tardo. Si supera el suelo sorteado no se espera nada:
     *            el suelo es un minimo, no una penalizacion fija
     */
    void applyDegradedFloor(long elapsedMillis);
}
