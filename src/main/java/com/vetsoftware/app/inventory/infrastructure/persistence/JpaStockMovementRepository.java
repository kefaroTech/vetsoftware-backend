package com.vetsoftware.app.inventory.infrastructure.persistence;

import com.vetsoftware.app.inventory.application.port.out.StockMovementRepository;
import com.vetsoftware.app.inventory.domain.StockMovement;
import com.vetsoftware.app.inventory.domain.StockMovementType;
import com.vetsoftware.app.inventory.domain.StockReferenceType;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class JpaStockMovementRepository implements StockMovementRepository {

  private final StockMovementJpaRepository jpaRepository;

  public JpaStockMovementRepository(StockMovementJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public StockMovement save(StockMovement movement) {
    return toDomain(jpaRepository.save(toJpa(movement)));
  }

  @Override
  public boolean existsByReference(StockReferenceType referenceType, Long referenceId) {
    if (referenceId == null) return false;
    return jpaRepository.existsByReferenceTypeAndReferenceId(referenceType.name(), referenceId);
  }

  @Override
  public List<StockMovement> findByReference(StockReferenceType referenceType, Long referenceId) {
    if (referenceId == null) return List.of();
    return jpaRepository
        .findByReferenceTypeAndReferenceId(referenceType.name(), referenceId)
        .stream()
        .map(JpaStockMovementRepository::toDomain)
        .toList();
  }

  private static StockMovementJpaEntity toJpa(StockMovement m) {
    StockMovementJpaEntity e = new StockMovementJpaEntity();
    e.setId(m.getId());
    e.setCompanyId(m.getCompanyId());
    e.setBranchId(m.getBranchId());
    e.setProductId(m.getProductId());
    e.setLotId(m.getLotId());
    e.setType(m.getType().name());
    e.setQuantity(m.getQuantity());
    e.setUnitCost(m.getUnitCost());
    e.setReferenceType(m.getReferenceType().name());
    e.setReferenceId(m.getReferenceId());
    e.setReason(m.getReason());
    e.setCreatedBy(m.getCreatedBy());
    e.setCreatedDate(m.getCreatedDate());
    return e;
  }

  private static StockMovement toDomain(StockMovementJpaEntity e) {
    return new StockMovement(
        e.getId(),
        e.getCompanyId(),
        e.getBranchId(),
        e.getProductId(),
        e.getLotId(),
        StockMovementType.valueOf(e.getType()),
        e.getQuantity(),
        e.getUnitCost(),
        StockReferenceType.valueOf(e.getReferenceType()),
        e.getReferenceId(),
        e.getReason(),
        e.getCreatedBy(),
        e.getCreatedDate());
  }
}
