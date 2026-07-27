package com.vetsoftware.app.catalogbarcode.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogBarcodeJpaRepository extends JpaRepository<CatalogBarcodeJpaEntity, Long> {
    @EntityGraph(attributePaths = {"presentation", "presentation.product", "bundle"})
    Optional<CatalogBarcodeJpaEntity> findByCompany_IdAndBarcode(Long companyId, String barcode);

    boolean existsByCompany_IdAndBarcode(Long companyId, String barcode);
    boolean existsByCompany_IdAndBarcodeAndIdNot(Long companyId, String barcode, Long id);
}
