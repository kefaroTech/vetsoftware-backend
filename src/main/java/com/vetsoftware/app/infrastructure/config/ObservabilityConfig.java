package com.vetsoftware.app.infrastructure.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Activa el soporte de {@code @Observed} sobre los servicios anotados.
 *
 * <h2>Aquí no va ningún ObservationPredicate</h2>
 *
 * <p>
 * Hubo uno ({@code productionActuatorObservationPredicate}) que descartaba las
 * observaciones de {@code /actuator/health} y {@code /actuator/prometheus} en
 * prod. Se retiró, y conviene dejar escrito por qué para no reponerlo:
 *
 * <ol>
 * <li><b>Filtraba de más y de menos a la vez.</b> Solo sabía reconocer
 * {@code ServerRequestObservationContext}; cualquier otro contexto pasaba de
 * largo. En un health check eso descartaba la observación HTTP pero no las de
 * Spring Security de esa misma petición, que se exportaban <em>huérfanas, sin
 * span padre</em>. Medido en Tempo: 38 fragmentos {@code security filterchain}
 * contra 12 trazas legítimas, un 76 % de basura. Parchearlo para reconocer más
 * tipos de contexto sería perseguir una lista que crece con cada versión de
 * Spring.</li>
 * <li><b>Ya no hace falta.</b> Nació cuando el muestreo se decidía dentro del
 * proceso, que solo puede mirar una observación a la vez y a ciegas. Hoy
 * Adaptive Traces muestrea por cola del lado de Grafana, viendo la traza
 * completa: un health check exitoso de 2 ms es exactamente lo que descarta, y
 * lo decide con información que aquí dentro no existe.</li>
 * </ol>
 *
 * <p>
 * El coste de quitarlo es enviar esos spans para que Grafana los tire: 0,33
 * spans/s contra un techo de ~1.250. Las observaciones de Spring Security se
 * conservan a propósito — en peticiones reales cuelgan correctamente del span
 * HTTP y ahí sí aportan.
 */
@Configuration
public class ObservabilityConfig {

    @Bean
    public ObservedAspect observedAspect(ObservationRegistry registry) {
        return new ObservedAspect(registry);
    }
}
