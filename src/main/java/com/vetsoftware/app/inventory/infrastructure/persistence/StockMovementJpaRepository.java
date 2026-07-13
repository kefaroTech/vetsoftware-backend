package com.vetsoftware.app.inventory.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementJpaRepository extends JpaRepository<StockMovementJpaEntity, Long> {
    boolean existsByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
    List<StockMovementJpaEntity> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);

    // Kardex paginado de un producto. branchId null = todas las sedes; from/to (inclusive/exclusivo) opcionales.
    @Query("SELECT m FROM StockMovementJpaEntity m WHERE m.companyId = :companyId AND m.productId = :productId "
        + "AND (:branchId IS NULL OR m.branchId = :branchId) "
        + "AND (:from IS NULL OR m.createdDate >= :from) AND (:to IS NULL OR m.createdDate < :to) "
        + "ORDER BY m.createdDate DESC, m.id DESC")
    Page<StockMovementJpaEntity> searchKardex(@Param("companyId") Long companyId, @Param("productId") Long productId,
                                              @Param("branchId") Long branchId, @Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to, Pageable pageable);

    // Libro de compras: movimientos de entrada real (type=PURCHASE) con nombre de producto/sede, paginado.
    @Query(value = "SELECT m.id AS id, m.product_id AS productId, p.name AS productName, p.code AS productCode, "
        + "m.lot_id AS lotId, m.branch_id AS branchId, b.name AS branchName, m.quantity AS quantity, "
        + "m.unit_cost AS unitCost, m.reference_id AS referenceId, m.created_date AS createdDate "
        + "FROM stock_movement m "
        + "JOIN products p ON p.id = m.product_id "
        + "JOIN branches b ON b.id = m.branch_id "
        + "WHERE m.company_id = :companyId AND m.type = 'PURCHASE' "
        + "AND (:branchId IS NULL OR m.branch_id = :branchId) "
        + "AND (:from IS NULL OR m.created_date >= :from) AND (:to IS NULL OR m.created_date < :to) "
        + "ORDER BY m.created_date DESC, m.id DESC",
        countQuery = "SELECT COUNT(*) FROM stock_movement m WHERE m.company_id = :companyId AND m.type = 'PURCHASE' "
        + "AND (:branchId IS NULL OR m.branch_id = :branchId) "
        + "AND (:from IS NULL OR m.created_date >= :from) AND (:to IS NULL OR m.created_date < :to)",
        nativeQuery = true)
    Page<PurchaseRow> searchPurchases(@Param("companyId") Long companyId, @Param("branchId") Long branchId,
                                      @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                      Pageable pageable);
}
