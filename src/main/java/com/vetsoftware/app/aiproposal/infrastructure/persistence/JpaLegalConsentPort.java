package com.vetsoftware.app.aiproposal.infrastructure.persistence;

import com.vetsoftware.app.aiproposal.application.port.out.LegalConsentPort;
import com.vetsoftware.app.aiproposal.domain.LegalDocumentVersionRef;
import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentKind;
import com.vetsoftware.app.legaldocumentversion.infrastructure.persistence.LegalDocumentAcceptanceJpaEntity;
import com.vetsoftware.app.legaldocumentversion.infrastructure.persistence.LegalDocumentAcceptanceJpaRepository;
import com.vetsoftware.app.legaldocumentversion.infrastructure.persistence.LegalDocumentVersionJpaEntity;
import com.vetsoftware.app.legaldocumentversion.infrastructure.persistence.LegalDocumentVersionJpaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * El unico fichero de esta rodaja que conoce las dos tablas legales.
 *
 * <p>
 * Es el patron canonico de referencia cruzada: el dominio de {@code aiproposal}
 * no importa nada de {@code legaldocumentversion} -tiene su companion VO
 * {@link LegalDocumentVersionRef}-, y el cruce vive aqui, en el adaptador, que
 * es donde el vertical slicing lo permite.
 *
 * <p>
 * <strong>{@code AI_PROPOSAL} y el id, nunca el token.</strong>
 * {@code subject_kind} distingue el universo -{@code COMPANY} y
 * {@code SYSTEM_USER} llegaran- y {@code subject_ref} lleva el
 * <strong>id</strong> de la propuesta: copiar el {@code public_token} a una
 * segunda tabla lo multiplicaria por dos y lo sacaria del control de acceso que
 * lo protege.
 */
@Component
public class JpaLegalConsentPort implements LegalConsentPort {

    /** Espejo de {@code chk_legal_document_acceptances_subject_kind}. */
    private static final String SUBJECT_KIND = "AI_PROPOSAL";

    private final LegalDocumentVersionJpaRepository versionRepository;

    private final LegalDocumentAcceptanceJpaRepository acceptanceRepository;

    private final Clock clock;

    public JpaLegalConsentPort(LegalDocumentVersionJpaRepository versionRepository,
            LegalDocumentAcceptanceJpaRepository acceptanceRepository, Clock clock) {
        this.versionRepository = versionRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.clock = clock;
    }

    @Override
    public Optional<LegalDocumentVersionRef> findVersion(String code, int documentVersion) {
        if (code == null || code.isBlank() || documentVersion < 1)
            return Optional.empty();
        return versionRepository.findByCodeAndDocumentVersion(code, documentVersion)
                .map(JpaLegalConsentPort::toRef);
    }

    /**
     * &#9940; <strong>Idempotente por el unico de la tabla.</strong> El front puede
     * reenviar el formulario -el boton de cancelar con {@code AbortController} hace
     * el doble envio mas probable, no menos- y una segunda fila para el mismo
     * {@code (sujeto, version)} chocaria contra
     * {@code uq_legal_document_acceptances_subject}, tumbando con un 500 una
     * peticion que hizo exactamente lo que se le pidio. La primera aceptacion es la
     * que vale: es la que lleva la fecha en la que de verdad se consintio.
     */
    @Override
    public void recordAcceptance(Long legalDocumentVersionId, Long subjectRef,
            LocalDateTime acceptedAt, String acceptedIpHash, String userAgentHash) {
        if (legalDocumentVersionId == null || subjectRef == null)
            throw new IllegalArgumentException("an acceptance needs its version and its subject");
        String sujeto = String.valueOf(subjectRef);
        if (acceptanceRepository.findBySubjectKindAndSubjectRefAndLegalDocumentVersionId(
                SUBJECT_KIND, sujeto, legalDocumentVersionId).isPresent())
            return;
        acceptanceRepository.save(
                new LegalDocumentAcceptanceJpaEntity(legalDocumentVersionId, SUBJECT_KIND, sujeto,
                        acceptedAt, acceptedIpHash, userAgentHash, LocalDateTime.now(clock)));
    }

    /**
     * <strong>El aviso se reconoce por {@code kind}, no por {@code code}.</strong>
     * El codigo es editorial y lo cambia negocio; {@code kind} es el vocabulario
     * cerrado de {@code chk_ldv_kind}, y las dos clases que amparan la recogida -la
     * politica de tratamiento y el aviso de privacidad- son las que pueden llenar
     * {@code ai_proposals.privacy_notice_version_id}.
     */
    private static LegalDocumentVersionRef toRef(LegalDocumentVersionJpaEntity entidad) {
        boolean aviso = entidad.getKind() == LegalDocumentKind.PRIVACY_POLICY
                || entidad.getKind() == LegalDocumentKind.PRIVACY_NOTICE;
        return new LegalDocumentVersionRef(entidad.getId(), entidad.getCode(),
                entidad.getDocumentVersion(), aviso);
    }
}
