package com.vetsoftware.app.catalogbarcode.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogBarcodeJpaRepository extends JpaRepository<CatalogBarcodeJpaEntity, Long> {
    @EntityGraph(attributePaths = {
        "presentation", "presentation.product", "presentation.unitMeasure",
        "bundle", "bundle.unitMeasure"
    })
    Optional<CatalogBarcodeJpaEntity> findByCompany_IdAndBarcode(Long companyId, String barcode);

    boolean existsByCompany_IdAndBarcode(Long companyId, String barcode);
    boolean existsByCompany_IdAndBarcodeAndIdNot(Long companyId, String barcode, Long id);

    List<CatalogBarcodeJpaEntity> findAllByCompany_IdAndPresentation_IdOrderByBarcode(
        Long companyId, Long presentationId);

    List<CatalogBarcodeJpaEntity> findAllByCompany_IdAndBundle_IdOrderByBarcode(
        Long companyId, Long bundleId);

    void deleteAllByCompany_IdAndPresentation_Id(Long companyId, Long presentationId);

    void deleteAllByCompany_IdAndBundle_Id(Long companyId, Long bundleId);
}
