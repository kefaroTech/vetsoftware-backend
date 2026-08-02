package com.vetsoftware.app.unitmeasure.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitMeasureCatalogJpaRepository
        extends
            JpaRepository<UnitMeasureCatalogJpaEntity, String> {
    List<UnitMeasureCatalogJpaEntity> findAllByOrderByNameAsc();
}
