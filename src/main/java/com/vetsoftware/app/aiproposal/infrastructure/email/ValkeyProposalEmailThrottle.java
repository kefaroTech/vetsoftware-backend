package com.vetsoftware.app.aiproposal.infrastructure.email;

import com.vetsoftware.app.aiproposal.application.port.out.ProposalEmailThrottlePort;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Un correo por destinatario y por hora, contado en Valkey.
 *
 * <p>
 * Reusa el {@code LettuceBasedProxyManager} que ya sostiene los limites de
 * {@code LoginRateLimitFilter}: es un bean de infraestructura de Bucket4j, no
 * un tipo de la rodaja {@code auth}, asi que inyectarlo desde aqui no cruza
 * ninguna frontera de dominio. Levantar un segundo cliente contra el mismo
 * Valkey solo añadiria conexiones.
 *
 * <p>
 * &#9940; <strong>La clave lleva el SHA-256 del correo en minusculas, nunca el
 * correo.</strong> Es la misma normalizacion que aplica la columna generada
 * {@code contact_email_hash}, asi que {@code Ana@X.com} y {@code ana@x.com}
 * comparten cupo -que es lo que hace util al limite: sin normalizar, cambiar
 * una mayuscula lo esquiva-. Y es un hash porque una clave de Redis no se
 * anonimiza a los 90 dias ni se purga a los 24 meses: meter ahi la direccion
 * seria sacar el dato personal justo del sitio donde la politica de retencion
 * lo alcanza.
 *
 * <p>
 * <strong>Fail-open, y esta escrito a proposito.</strong> Si Valkey no
 * responde, el correo sale. El riesgo de no limitar durante una caida de Redis
 * es que alguien mande correos de mas; el de fallar cerrado es que ningun
 * prospecto reciba su enlace mientras dure la incidencia, con la propuesta ya
 * cobrada al modelo y guardada. Es la decision opuesta a la del tope de gasto
 * -que si es fail-closed- porque lo que hay al otro lado es distinto: alli,
 * dinero.
 */
@Component
public class ValkeyProposalEmailThrottle implements ProposalEmailThrottlePort {

    private static final Logger log = LoggerFactory.getLogger(ValkeyProposalEmailThrottle.class);

    static final String KEY_PREFIX = "ai-proposal-mail-rl:";

    /** Un correo por hora y por destinatario. */
    static final int CUPO = 1;

    static final Duration VENTANA = Duration.ofHours(1);

    private final LettuceBasedProxyManager<String> proxyManager;

    public ValkeyProposalEmailThrottle(
            LettuceBasedProxyManager<String> loginRateLimitProxyManager) {
        this.proxyManager = loginRateLimitProxyManager;
    }

    @Override
    public boolean tryAcquire(String contactEmail) {
        if (contactEmail == null || contactEmail.isBlank()) {
            return false;
        }
        try {
            BucketProxy bucket = proxyManager.builder().build(KEY_PREFIX + hash(contactEmail),
                    ValkeyProposalEmailThrottle::configuracion);
            return bucket.tryConsume(1);
        } catch (RuntimeException fallo) {
            // Sin el correo y sin el hash: el mensaje de un fallo de Redis no es sitio
            // para ninguno de los dos.
            log.warn("No se pudo consultar el cupo de correo de la propuesta; se deja pasar: {}",
                    fallo.getMessage());
            return true;
        }
    }

    private static BucketConfiguration configuracion() {
        return BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(CUPO).refillIntervally(CUPO, VENTANA)).build();
    }

    static String hash(String contactEmail) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(
                    contactEmail.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException imposible) {
            // SHA-256 es obligatorio en toda JVM. Si falta, el entorno esta roto de una
            // forma que no tiene sentido tratar aqui.
            throw new IllegalStateException("SHA-256 no disponible", imposible);
        }
    }
}
