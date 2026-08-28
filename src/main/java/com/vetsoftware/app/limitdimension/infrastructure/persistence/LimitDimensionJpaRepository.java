package com.vetsoftware.app.limitdimension.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Consultas del catálogo de ejes. Ninguna se acota por empresa porque la tabla
 * no la tiene: es catálogo global de plataforma, y quien las consume va cerrado
 * a un principal cross-tenant.
 */
public interface LimitDimensionJpaRepository extends JpaRepository<LimitDimensionJpaEntity, Long> {

    Optional<LimitDimensionJpaEntity> findByCode(String code);

    boolean existsByCode(String code);

    List<LimitDimensionJpaEntity> findAllByOrderByCodeAsc();
}
