package com.vetsoftware.app.documentwithholding.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <strong>Sin ninguna {@code @Query} de {@code UPDATE} ni de
 * {@code DELETE}</strong>, y no es que aun no hayan hecho falta. La tabla solo
 * se agrega, y su unica mutacion —apuntar {@code certificate_id}— pasa por la
 * entidad gestionada, que es donde {@code @Version} compara la version en el
 * {@code WHERE} y la incrementa en el {@code SET}. Una {@code @Query} de
 * {@code UPDATE} iria directa a la base sin comprobar ni incrementar nada, y el
 * {@code save} concurrente que llegara con la version vieja casaria igual y
 * pisaria el cambio, sin excepcion, sin log y sin 409
 * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}).
 *
 * <p>
 * Las dos consultas de vigilancia van <strong>derivadas</strong> y no en JPQL a
 * mano: {@code CertificateIdIsNull} es literalmente el estado «sin respaldo», y
 * escrito asi no hay forma de que un {@code = null} accidental —que en SQL no
 * casa nunca— vacie la bandeja de reclamaciones sin que nadie lo note.
 */
public interface DocumentWithholdingJpaRepository
        extends
            JpaRepository<DocumentWithholdingJpaEntity, Long> {

    Optional<DocumentWithholdingJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

    Page<DocumentWithholdingJpaEntity> findAllByCompanyId(Long companyId, Pageable pageable);

    /**
     * Barrido de vigilancia sin tenant delante. Sirve el indice
     * {@code ix_document_withholdings_certificate} solo parcialmente —no lleva el
     * ano— y por eso el filtro por {@code fiscalYear} es obligatorio y no opcional:
     * sin el, la consulta recorre la tabla entera.
     *
     * <p>
     * Solo lo alcanza un caso de uso cerrado a {@code hasRole('SYSTEM')} a secas.
     */
    Page<DocumentWithholdingJpaEntity> findAllByFiscalYearAndCertificateIdIsNull(short fiscalYear,
            Pageable pageable);

    /** La misma vigilancia acotada por empresa, que es la que ve el cliente. */
    Page<DocumentWithholdingJpaEntity> findAllByCompanyIdAndFiscalYearAndCertificateIdIsNull(
            Long companyId, short fiscalYear, Pageable pageable);
}
