package com.vetsoftware.app.infrastructure.observability;

import jakarta.annotation.PostConstruct;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Escribe una sola línea en el arranque diciendo qué series de latencia no se
 * publican y dónde está la equivalente.
 *
 * <p>
 * <b>Existe porque un filtro que deniega es invisible.</b> El defecto que este
 * paquete entero persigue —un hueco indistinguible de la ausencia de actividad—
 * lo puede producir igual de bien una decisión correcta que un error, y la
 * única diferencia operativa entre las dos es que la correcta esté escrita en
 * algún sitio donde alguien la encuentre a las tres de la mañana. El log del
 * proceso es ese sitio; el javadoc no lo es.
 *
 * <p>
 * {@code INFO} según el árbol de decisión del repositorio: es un hecho normal
 * del ciclo de vida —configuración aplicada— que un operador querrá ver en
 * producción. Un evento por arranque, no un bucle.
 */
public final class ReadObservationMeterFilterAnnouncer {

    private static final Logger log = LoggerFactory
            .getLogger(ReadObservationMeterFilterAnnouncer.class);

    @PostConstruct
    void announce() {
        List<String> denied = ReadObservationMeterFilter.DENIED_READ_OBSERVATIONS.stream().sorted()
                .toList();
        log.info(
                "Filtro de cardinalidad de lecturas activo: {} observaciones del bloque de"
                        + " suscripciones no publican Timer ni LongTaskTimer (~{} series"
                        + " ahorradas). Su latencia se consulta en"
                        + " http_server_requests_seconds por uri; el span de la traza sigue"
                        + " emitiéndose. Observaciones afectadas: {}",
                denied.size(), denied.size() * 8, String.join(", ", denied));
    }
}
