package com.vetsoftware.app.aiproposal.infrastructure.persistence;

import com.vetsoftware.app.aiproposal.application.port.out.ProposalRetentionPort;
import java.time.LocalDateTime;
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

    private final AiProposalRetentionJpaRepository jpaRepository;

    public JpaProposalRetentionPort(AiProposalRetentionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
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
     */
    @Override
    @Transactional
    public SuppressionResult suppressByContactEmail(String contactEmail, LocalDateTime ahora) {
        if (contactEmail == null || contactEmail.isBlank()) {
            return new SuppressionResult(0, 0, 0);
        }
        int lineas = jpaRepository.suppressLinesByEmail(contactEmail, ahora);
        int turnos = jpaRepository.suppressTurnsByEmail(contactEmail);
        int propuestas = jpaRepository.suppressProposalsByEmail(contactEmail, ahora);
        return new SuppressionResult(propuestas, turnos, lineas);
    }
}
