package com.vetsoftware.app.smmlvvalue.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmmlvValueJpaRepository extends JpaRepository<SmmlvValueJpaEntity, Long> {

    Optional<SmmlvValueJpaEntity> findByFiscalYear(short fiscalYear);

    boolean existsByFiscalYear(short fiscalYear);
}
