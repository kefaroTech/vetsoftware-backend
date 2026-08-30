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
import java.util.Set;
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
 * &#9940; <strong>La clave lleva el SHA-256 del correo canonicalizado, nunca el
 * correo.</strong> Canonicalizar es mas que bajar a minusculas
 * —{@code Ana@X.com} y {@code ana@x.com} comparten cupo, pero tambien
 * {@code ana+1@x.com}: ver {@link #canonicalizar(String)}, donde esta el
 * argumento y sus limites—. Ojo a la diferencia con la columna generada
 * {@code contact_email_hash}, que solo baja a minusculas: aquella identifica
 * <em>la fila</em> y esta cuenta <em>envios a un buzon</em>, que no son la
 * misma pregunta.
 *
 * <p>
 * Y es un hash porque una clave de Redis no se anonimiza a los 90 dias ni se
 * purga a los 24 meses: meter ahi la direccion seria sacar el dato personal
 * justo del sitio donde la politica de retencion lo alcanza.
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

    /**
     * Los dominios donde el punto del local-part <strong>no</strong> distingue
     * buzones. Google lo documenta para {@code gmail.com} y su alias historico
     * {@code googlemail.com}, y hasta donde se puede afirmar por documentacion del
     * proveedor son los unicos: en el resto, {@code a.b@} y {@code ab@} son dos
     * personas. La lista es corta a proposito —anadir un dominio aqui por
     * suposicion funde dos buzones reales en un cupo de 1/hora y deja a uno de los
     * dos sin su propuesta—.
     */
    private static final Set<String> DOMINIOS_QUE_IGNORAN_LOS_PUNTOS = Set.of("gmail.com",
            "googlemail.com");

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
            return HexFormat.of().formatHex(
                    digest.digest(canonicalizar(contactEmail).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException imposible) {
            // SHA-256 es obligatorio en toda JVM. Si falta, el entorno esta roto de una
            // forma que no tiene sentido tratar aqui.
            throw new IllegalStateException("SHA-256 no disponible", imposible);
        }
    }

    /**
     * &#9940; <strong>Minusculas no basta: el cupo se saltaba con
     * subdirecciones.</strong> {@code victima+1@gmail.com},
     * {@code victima+2@gmail.com} y {@code v.i.c.t.i.m.a@gmail.com} son tres claves
     * distintas y <strong>un solo buzon</strong>. Con un cupo de 1/hora, un
     * atacante que itera el sufijo manda un correo por peticion a la victima con
     * nuestro remitente y nuestra reputacion, y el estrangulador nunca se entera.
     *
     * <p>
     * Dos normalizaciones, y no son igual de universales:
     *
     * <ul>
     * <li><strong>La etiqueta tras {@code +} se quita siempre.</strong> Es la
     * convencion de subdireccion; en los proveedores que no la implementan, el
     * {@code +} es un caracter legal del local-part y quitarlo puede fundir dos
     * buzones que de verdad son distintos. Se acepta ese error: el precio es que
     * dos personas compartan un cupo de un correo por hora, contra permitir el
     * envio ilimitado a un tercero.</li>
     * <li><strong>Los puntos se quitan SOLO donde son irrelevantes.</strong> Gmail
     * los ignora por documentacion propia; en la inmensa mayoria de los dominios
     * <em>no</em> lo son —{@code a.b@empresa.com} y {@code ab@empresa.com} son dos
     * personas distintas—, y quitarlos en general fundiria los buzones de dos
     * empleados en un unico cupo, con el efecto de que uno de los dos no recibe su
     * propuesta. Por eso va contra una lista corta y explicita.</li>
     * </ul>
     *
     * <p>
     * <strong>Esto es la clave del cupo, no la direccion de envio.</strong> El
     * correo sale a la direccion que escribio el prospecto, tal cual; lo unico que
     * se canonicaliza es lo que se resume para contar.
     */
    static String canonicalizar(String contactEmail) {
        String normalizado = contactEmail.trim().toLowerCase(Locale.ROOT);
        int arroba = normalizado.lastIndexOf('@');
        if (arroba <= 0 || arroba == normalizado.length() - 1)
            return normalizado;
        String local = normalizado.substring(0, arroba);
        String dominio = normalizado.substring(arroba + 1);
        int etiqueta = local.indexOf('+');
        if (etiqueta >= 0)
            local = local.substring(0, etiqueta);
        if (DOMINIOS_QUE_IGNORAN_LOS_PUNTOS.contains(dominio))
            local = local.replace(".", "");
        // Un local-part que era solo la etiqueta ("+algo@x") se queda vacio: se
        // devuelve el original para no fundir todos esos casos en una unica clave.
        return local.isEmpty() ? normalizado : local + "@" + dominio;
    }
}
