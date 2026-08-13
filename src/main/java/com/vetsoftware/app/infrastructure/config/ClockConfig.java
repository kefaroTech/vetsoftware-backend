package com.vetsoftware.app.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj del sistema como bean inyectable.
 *
 * <p>
 * Existe por la regla de determinismo del CLAUDE.md: un service que llame a
 * {@code LocalDate.now()} directamente no se puede probar sin que el test
 * dependa del reloj de la maquina —y se caiga solo el dia que la medianoche
 * caiga entre dos lineas—. Con el reloj inyectado, el test usa
 * {@code Clock.fixed(...)} y el caso queda fijado para siempre.
 *
 * <p>
 * Se declara una sola vez y en el paquete de configuracion transversal porque
 * no pertenece a ninguna feature: es infraestructura del runtime, como el
 * planificador o el pool de hilos.
 */
@Configuration
public class ClockConfig {
    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
