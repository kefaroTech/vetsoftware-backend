package com.vetsoftware.app.uvtvalue.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UvtValueJpaRepository extends JpaRepository<UvtValueJpaEntity, Long> {

    Optional<UvtValueJpaEntity> findByFiscalYear(short fiscalYear);

    boolean existsByFiscalYear(short fiscalYear);
}
