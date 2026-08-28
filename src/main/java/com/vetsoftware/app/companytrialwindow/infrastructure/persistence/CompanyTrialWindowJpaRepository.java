package com.vetsoftware.app.companytrialwindow.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Consultas del reloj de la empresa, todas acotadas por empresa.
 *
 * <p>
 * No declara ninguna escritura masiva: mover el fin de una ventana con
 * concesiones colgando es un error del motor por diseño
 * ({@code ON UPDATE RESTRICT}), y una consulta de actualización que lo
 * intentara solo serviría para descubrirlo en producción.
 */
public interface CompanyTrialWindowJpaRepository
        extends
            JpaRepository<CompanyTrialWindowJpaEntity, Long> {

    Optional<CompanyTrialWindowJpaEntity> findByCompanyIdAndClosedAtIsNull(Long companyId);

    Optional<CompanyTrialWindowJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByCompanyIdAndClosedAtIsNull(Long companyId);
}
