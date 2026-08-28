package com.vetsoftware.app.withholdingcertificate.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <strong>Sin ninguna {@code @Query} de {@code UPDATE} ni de {@code DELETE}, y
 * no es que aun no hayan hecho falta.</strong> Las dos segundas escrituras de
 * esta ficha -recibir el certificado y adjuntar el sustituto- pasan por el
 * agregado cargado y su {@code save}, que es el unico camino donde
 * {@code @Version} protege de verdad: un {@code UPDATE} masivo va directo a la
 * base, ni comprueba ni incrementa la version, y deja que un {@code save}
 * concurrente con la version vieja pise el cambio sin excepcion y sin log
 * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}, incidencia #53).
 *
 * <p>
 * <strong>{@link #findByIdAndCompanyId(Long, Long)} es contrato publico de esta
 * clase.</strong> El adaptador de {@code documentwithholding} lo consume por
 * nombre y firma para comprobar que el certificado al que apunta una retencion
 * es de la misma empresa; renombrar cualquiera de los dos rompe su compilacion,
 * no la de aqui.
 */
public interface WithholdingCertificateJpaRepository
        extends
            JpaRepository<WithholdingCertificateJpaEntity, Long> {

    Optional<WithholdingCertificateJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

    Page<WithholdingCertificateJpaEntity> findAllByCompanyId(Long companyId, Pageable pageable);

    /**
     * Los que vencen antes de una fecha y siguen sin llegar, de todas las empresas.
     * Es un barrido cross-tenant y por eso solo lo alcanza un puerto
     * {@code hasRole('SYSTEM')} a secas: acotar por vencimiento no acota por
     * tenant, una fecha no es de nadie ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}).
     *
     * <p>
     * Las dos columnas y en ese orden son las de
     * {@code ix_withholding_certificates_missing}, que es el indice que esta
     * consulta existe para usar.
     */
    Page<WithholdingCertificateJpaEntity> findAllByLegalDeadlineOnLessThanAndReceivedOnIsNull(
            LocalDate deadlineBefore, Pageable pageable);

    /** La sobrecarga acotada: lo mismo con la empresa delante. */
    Page<WithholdingCertificateJpaEntity> findAllByCompanyIdAndLegalDeadlineOnLessThanAndReceivedOnIsNull(
            Long companyId, LocalDate deadlineBefore, Pageable pageable);
}
