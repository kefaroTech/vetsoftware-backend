package com.vetsoftware.app.aiproposal.infrastructure.ai;

import com.vetsoftware.app.aiproposal.application.port.out.ResponsePacingPort;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * El suelo de latencia de la ruta degradada, sorteado en cada respuesta.
 *
 * <p>
 * &#9940; <strong>El canal que cierra.</strong> Una generacion real tarda 3-8
 * segundos; una degradacion por tope de gasto o por palanca vuelve en
 * milisegundos. Sin suelo, cualquiera con {@code curl} y un cronometro sabe
 * -sin credenciales, sin cuenta y sin tocar nada- <strong>cuando se agoto el
 * presupuesto diario de la plataforma</strong>, que es justo la informacion con
 * la que se decide cuando vaciarlo barato.
 *
 * <p>
 * <strong>Aleatorio y no fijo.</strong> Un suelo constante de 3.000 ms se
 * reconoce igual de bien que uno de 5: lo que delata no es la magnitud, es la
 * <em>varianza</em>. Un rango uniforme de 2.500-4.500 ms solapa con la parte
 * baja de la distribucion real, que es todo lo que se puede pedir sin pagar la
 * espera entera.
 *
 * <p>
 * &#9888; <strong>Y bloquea el hilo del servlet, que es el coste conocido de
 * esta decision.</strong> El plan pedia evitarlo "si se puede"; con la
 * respuesta sincrona que devuelven los cuatro endpoints, la alternativa es un
 * {@code DeferredResult} y un planificador, es decir mover la feature entera a
 * asincrono por un control de tres lineas. Con hilos virtuales el coste de un
 * hilo dormido es una constante pequena; sin ellos, el techo lo pone el
 * {@code RouteLimit} de la ruta -5/hora por IP-, que es lo que impide que esto
 * sea a la vez el canal cerrado y una negacion de servicio abierta.
 */
@Component
public class RandomizedResponsePacing implements ResponsePacingPort {

    private static final Logger log = LoggerFactory.getLogger(RandomizedResponsePacing.class);

    private final long sueloMinimoMs;

    private final long sueloMaximoMs;

    public RandomizedResponsePacing(
            @Value("${vetsoftware.ai.proposal.degraded-floor-min-ms:2500}") long sueloMinimoMs,
            @Value("${vetsoftware.ai.proposal.degraded-floor-max-ms:4500}") long sueloMaximoMs) {
        if (sueloMinimoMs < 0 || sueloMaximoMs < sueloMinimoMs)
            throw new IllegalArgumentException("the degraded floor range is not usable");
        this.sueloMinimoMs = sueloMinimoMs;
        this.sueloMaximoMs = sueloMaximoMs;
    }

    @Override
    public void applyDegradedFloor(long elapsedMillis) {
        long suelo = sueloMinimoMs == sueloMaximoMs
                ? sueloMinimoMs
                : ThreadLocalRandom.current().nextLong(sueloMinimoMs, sueloMaximoMs + 1);
        long queda = suelo - Math.max(0, elapsedMillis);
        if (queda <= 0)
            return;
        try {
            Thread.sleep(queda);
        } catch (InterruptedException interrumpido) {
            // Reponer la marca y salir: quien interrumpe quiere que el hilo termine,
            // y tragarse la interrupcion aqui dejaria el apagado ordenado colgado
            // hasta cuatro segundos y medio por peticion en vuelo.
            Thread.currentThread().interrupt();
            log.debug("Suelo de latencia interrumpido tras {} ms", elapsedMillis);
        }
    }
}
