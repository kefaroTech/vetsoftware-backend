package com.vetsoftware.app.companylimitevent.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Consultas de la bitácora, acotadas por empresa.
 *
 * <p>
 * Sin consultas de escritura: la tabla solo se agrega. Un {@code UPDATE} aquí
 * sería reescribir una prueba.
 */
public interface CompanyLimitEventJpaRepository
        extends
            JpaRepository<CompanyLimitEventJpaEntity, Long> {

    List<CompanyLimitEventJpaEntity> findAllByCompanyIdAndOccurredAtBetweenOrderByOccurredAtAscIdAsc(
            Long companyId, LocalDateTime from, LocalDateTime to);
}
