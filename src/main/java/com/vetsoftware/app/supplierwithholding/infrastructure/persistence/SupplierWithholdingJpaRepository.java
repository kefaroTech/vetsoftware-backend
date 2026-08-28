package com.vetsoftware.app.supplierwithholding.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <strong>Sin una sola {@code @Query}.</strong> Las dos consultas del negocio
 * las expresa el derivador de nombres de Spring Data, asi que aqui no hay SQL
 * que pueda olvidarse de mover la {@code version} en su {@code SET}
 * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}) ni proyectar un literal booleano
 * ({@code PROYECCION_SIN_LITERAL_BOOLEANO}). Las dos escrituras que editan
 * —emitir el certificado y anotar el acuse— pasan por el ciclo
 * leer-modificar-guardar, que es el unico camino que {@code @Version} protege.
 *
 * <p>
 * Ningun metodo recibe {@code companyId} porque la tabla no tiene esa columna.
 */
public interface SupplierWithholdingJpaRepository
        extends
            JpaRepository<SupplierWithholdingJpaEntity, Long> {

    /** Armar la declaracion del mes. Sirve a {@code ix_sw_declaration}. */
    Page<SupplierWithholdingJpaEntity> findAllByFiscalPeriodKey(String fiscalPeriodKey,
            Pageable pageable);

    /**
     * El certificado anual del proveedor. Sirve a {@code ix_sw_certificate}, y el
     * año va como {@code short} porque la columna es {@code SMALLINT}: pasar un
     * {@code int} obligaria a Hibernate a convertir en cada llamada y, peor, a no
     * poder usar el indice si el motor decidiera promover la columna.
     */
    Page<SupplierWithholdingJpaEntity> findAllBySupplierTaxIdAndFiscalYear(String supplierTaxId,
            short fiscalYear, Pageable pageable);
}
