package com.vetsoftware.app.legaldocumentversion.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Las aceptaciones de textos legales.
 *
 * <p>
 * &#9940; <strong>Ni un metodo con {@code companyId} ni con {@code Company} en
 * el nombre.</strong> Esa es la senal exacta con la que
 * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} decide si un repositorio "sabe
 * filtrar por empresa", y basta uno para que todo {@code find...} suyo que
 * devuelva varias filas pase a exigir {@code hasRole('SYSTEM')} a secas -que es
 * justo lo que un prospecto anonimo no puede tener-. El dia que se acepte un
 * documento como empresa, el sujeto viaja en {@code subject_ref} como cualquier
 * otro.
 */
public interface LegalDocumentAcceptanceJpaRepository
        extends
            JpaRepository<LegalDocumentAcceptanceJpaEntity, Long> {

    /**
     * La aceptacion ya registrada, si la hay. Existe para que un reintento del
     * cliente no choque contra {@code uq_legal_document_acceptances_subject}:
     * aceptar dos veces el mismo texto es idempotente, no un error.
     */
    Optional<LegalDocumentAcceptanceJpaEntity> findBySubjectKindAndSubjectRefAndLegalDocumentVersionId(
            String subjectKind, String subjectRef, Long legalDocumentVersionId);
}
