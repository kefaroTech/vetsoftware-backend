package com.vetsoftware.app.inventory.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryCountJpaRepository extends JpaRepository<InventoryCountJpaEntity, Long> {

  // Historial por empresa y (opcional) sede, más reciente primero. Sin cargar líneas (LAZY): el
  // listado solo usa
  // los contadores desnormalizados. @SQLRestriction filtra enabled=true.
  @Query(
      "SELECT c FROM InventoryCountJpaEntity c WHERE c.companyId = :companyId "
          + "AND (:branchId IS NULL OR c.branchId = :branchId) ORDER BY c.createdDate DESC")
  Page<InventoryCountJpaEntity> search(
      @Param("companyId") Long companyId, @Param("branchId") Long branchId, Pageable pageable);

  // Detalle con líneas (una sola consulta, evita N+1). Valida pertenencia a la empresa.
  @EntityGraph(attributePaths = "lines")
  Optional<InventoryCountJpaEntity> findByIdAndCompanyId(Long id, Long companyId);
}
