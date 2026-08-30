package com.vetsoftware.app.aiproposal.infrastructure.persistence;

import com.vetsoftware.app.aiproposal.application.port.out.ProposalRetentionPort;
import com.vetsoftware.app.legaldocumentversion.infrastructure.persistence.LegalDocumentAcceptanceJpaRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador de la retencion.
 *
 * <p>
 * <strong>El {@code @Transactional} esta aqui y por metodo, no envolviendo el
 * barrido.</strong> Cada lote es su propia transaccion: asi los bloqueos duran
 * lo que dura un lote, un fallo en el lote 40 conserva los 39 anteriores, y el
 * barrido puede reanudarse en la pasada siguiente sin repetir trabajo. Una
 * transaccion alrededor de la pasada entera tendria las tres tablas bloqueadas
 * durante minutos y perderia todo el avance ante cualquier error.
 */
@Component
public class JpaProposalRetentionPort implements ProposalRetentionPort {

    /** El mismo algoritmo que {@code SHA2(..., 256)} de la columna generada. */
    private static final String ALGORITMO_DE_HUELLA = "SHA-256";

    private final AiProposalRetentionJpaRepository jpaRepository;

    private final LegalDocumentAcceptanceJpaRepository acceptanceRepository;

    private final AiProposalSuppressionRequestJpaRepository suppressionRepository;

    public JpaProposalRetentionPort(AiProposalRetentionJpaRepository jpaRepository,
            LegalDocumentAcceptanceJpaRepository acceptanceRepository,
            AiProposalSuppressionRequestJpaRepository suppressionRepository) {
        this.jpaRepository = jpaRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.suppressionRepository = suppressionRepository;
    }

    @Override
    @Transactional
    public int anonymizeProposals(LocalDateTime inactivasDesde, LocalDateTime ahora,
            int tamanoDeLote) {
        return jpaRepository.anonymizeProposals(inactivasDesde, ahora, tamanoDeLote);
    }

    @Override
    @Transactional
    public int redactTurns(int tamanoDeLote) {
        return jpaRepository.redactTurns(tamanoDeLote);
    }

    @Override
    @Transactional
    public int redactLineReasons(LocalDateTime ahora, int tamanoDeLote) {
        return jpaRepository.redactLineReasons(ahora, tamanoDeLote);
    }

    @Override
    @Transactional
    public int purgeLines(LocalDateTime anterioresA, int tamanoDeLote) {
        return jpaRepository.purgeLines(anterioresA, tamanoDeLote);
    }

    @Override
    @Transactional
    public int purgeTurns(LocalDateTime anterioresA, int tamanoDeLote) {
        return jpaRepository.purgeTurns(anterioresA, tamanoDeLote);
    }

    /**
     * &#9940; <strong>Cruza a la rodaja legal, y es el unico cruce
     * permitido.</strong> La consulta vive en
     * {@code LegalDocumentAcceptanceJpaRepository} —la rodaja duena de la tabla— y
     * este adaptador solo la invoca: es el patron canonico de referencia cruzada,
     * el mismo que ya usa {@code JpaLegalConsentPort} para escribir la aceptacion.
     */
    @Override
    @Transactional
    public int purgeAcceptances(LocalDateTime anterioresA, int tamanoDeLote) {
        return acceptanceRepository.purgeProposalAcceptances(anterioresA, tamanoDeLote);
    }

    @Override
    @Transactional
    public int purgeProposals(LocalDateTime anterioresA, int tamanoDeLote) {
        return jpaRepository.purgeProposals(anterioresA, tamanoDeLote);
    }

    /**
     * &#9940; <strong>El orden de los tres pasos es al reves que el del
     * barrido</strong> y es lo unico que hace que la supresion funcione: el paso
     * que borra {@code contact_email} destruye {@code contact_email_hash}, que es
     * una columna generada y es por lo que buscan los otros dos. Invertirlo deja
     * los motivos del titular escritos, con el barrido informando exito y el
     * informe de cumplimiento diciendo que se le borro.
     *
     * <p>
     * <strong>Los tres van en la MISMA transaccion</strong>, no una por paso: una
     * supresion a medias -correo borrado, motivos dentro- es exactamente el estado
     * que no se puede detectar despues, porque ya no queda hash con el que volver a
     * buscarlos.
     *
     * <p>
     * &#9940; <strong>Y la fila de evidencia va en esa misma transaccion, no en una
     * aparte ni en un {@code afterCommit}.</strong> Es la parte critica de este
     * metodo: si la evidencia commiteara por su cuenta, existirian dos estados que
     * nadie puede detectar despues —un borrado sin prueba, y una prueba de un
     * borrado que revirtio—. El segundo es el peor: afirma por escrito ante la SIC
     * algo que no ocurrio. Por eso la escritura de evidencia <strong>no lleva
     * try/catch</strong>: si falla, tiene que llevarse los borrados por delante.
     *
     * <p>
     * <strong>El orden dentro de la transaccion tampoco es libre.</strong> La
     * lectura de la peticion anterior va primero de todo, porque despues del
     * {@code save} la fila recien escrita seria su propia predecesora; y el
     * {@code save} va al final porque necesita los tres contadores.
     */
    @Override
    @Transactional
    public SuppressionResult suppressByContactEmail(String contactEmail,
            Long executedBySystemUserId, LocalDateTime ahora) {
        if (contactEmail == null || contactEmail.isBlank()) {
            return new SuppressionResult(0, 0, 0, null);
        }
        byte[] huella = huellaDelCorreo(contactEmail);
        LocalDateTime anterior = suppressionRepository.findLastExecutedAt(huella).orElse(null);

        int lineas = jpaRepository.suppressLinesByEmail(contactEmail, ahora);
        int turnos = jpaRepository.suppressTurnsByEmail(contactEmail);
        int propuestas = jpaRepository.suppressProposalsByEmail(contactEmail, ahora);

        suppressionRepository.save(new AiProposalSuppressionRequestJpaEntity(huella, ahora,
                executedBySystemUserId, propuestas, turnos, lineas, ahora));
        return new SuppressionResult(propuestas, turnos, lineas, anterior);
    }

    /**
     * &#9940; <strong>Byte a byte lo que calcula
     * {@code UNHEX(SHA2(LOWER(contact_email), 256))}</strong> en la columna
     * generada de {@code ai_proposals}, que es la misma expresion que usan las tres
     * consultas de supresion. Cualquier diferencia -un {@code trim}, otra
     * codificacion, una sal- rompe en silencio el emparejamiento entre una peticion
     * repetida y su predecesora, que es la unica razon por la que esta columna
     * existe.
     *
     * <p>
     * <strong>Sin HMAC y sin pimienta a proposito.</strong> Una pimienta perdida o
     * rotada dejaria de emparejar sin que nada fallara: la tabla seguiria
     * llenandose y cada peticion repetida se leeria como la primera.
     */
    private static byte[] huellaDelCorreo(String contactEmail) {
        try {
            return MessageDigest.getInstance(ALGORITMO_DE_HUELLA)
                    .digest(contactEmail.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("SHA-256 is mandatory in every JVM", imposible);
        }
    }
}
