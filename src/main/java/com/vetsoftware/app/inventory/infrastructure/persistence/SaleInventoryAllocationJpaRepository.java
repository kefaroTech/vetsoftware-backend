package com.vetsoftware.app.inventory.infrastructure.persistence;

import com.vetsoftware.app.inventory.domain.StockReferenceType;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleInventoryAllocationJpaRepository
        extends JpaRepository<SaleInventoryAllocationJpaEntity, Long> {
    boolean existsByCompany_IdAndReferenceTypeAndReferenceIdAndCommercialLineKeyAndComponentSequence(
        Long companyId, StockReferenceType referenceType, Long referenceId, String commercialLineKey,
        int componentSequence);

    @EntityGraph(attributePaths = {"branch", "sourcePresentation", "sourceBundle", "componentPresentation",
        "componentProduct"})
    List<SaleInventoryAllocationJpaEntity>
        findAllByCompany_IdAndReferenceTypeAndReferenceIdOrderByCommercialLineKeyAscComponentSequenceAsc(
            Long companyId, StockReferenceType referenceType, Long referenceId);
}
