package com.vetsoftware.app.legaldocumentversion.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * &#9940; <strong>Las aceptaciones de las propuestas que la purga se va a
     * llevar.</strong> Vive aqui —en la rodaja duena de la tabla— y no en el
     * repositorio de retencion de {@code aiproposal}, aunque sea el barrido de
     * aquella rodaja quien la invoca a traves de su puerto.
     *
     * <p>
     * <strong>El predicado es el mismo que el de {@code purgeProposals}</strong>, y
     * tiene que serlo: si divergieran, o quedarian huerfanas -el defecto que esto
     * cierra- o se borraria la evidencia de una propuesta que sigue viva. Incluida
     * la guarda de conversion: una propuesta que acabo en cliente no se purga
     * nunca, y su consentimiento tampoco.
     *
     * <p>
     * <strong>El {@code CAST} es inevitable y esta del lado correcto.</strong>
     * {@code subject_ref} es {@code VARCHAR(64)} porque el sujeto es polimorfico;
     * se convierte la <em>columna</em> a entero y no al reves porque un
     * {@code subject_ref} no numerico -no existe hoy, pero el vocabulario de
     * {@code subject_kind} admite tres universos- daria {@code 0} y no casaria con
     * ningun id. El {@code subject_kind} acota la busqueda a las propuestas antes
     * de llegar al {@code CAST}.
     *
     * <p>
     * <strong>{@code ORDER BY id LIMIT}</strong> como el resto del barrido: sin el,
     * la primera pasada despues de un pico de trafico borraria cientos de miles de
     * filas en una sola transaccion.
     *
     * <p>
     * <strong>Sin {@code version} que mover</strong>
     * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}): es un {@code DELETE}, se lleva la
     * fila, y ademas la tabla va exenta con {@code E1_APPEND_ONLY}.
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            DELETE FROM legal_document_acceptances
             WHERE subject_kind = 'AI_PROPOSAL'
               AND CAST(subject_ref AS UNSIGNED) IN (
                     SELECT p.id FROM ai_proposals p
                      WHERE p.last_activity_at < :anterioresA
                        AND NOT EXISTS (SELECT 1 FROM ai_proposal_conversions c
                                         WHERE c.proposal_id = p.id))
             ORDER BY id
             LIMIT :tamanoDeLote
            """)
    int purgeProposalAcceptances(@Param("anterioresA") LocalDateTime anterioresA,
            @Param("tamanoDeLote") int tamanoDeLote);
}
