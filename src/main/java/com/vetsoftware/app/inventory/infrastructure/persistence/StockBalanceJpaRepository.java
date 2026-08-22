package com.vetsoftware.app.inventory.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockBalanceJpaRepository extends JpaRepository<StockBalanceJpaEntity, Long> {

    Optional<StockBalanceJpaEntity> findByProductIdAndBranchId(Long productId, Long branchId);

    // Lock pesimista (SELECT ... FOR UPDATE): serializa las salidas concurrentes
    // del mismo (producto,
    // sede).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b
            FROM StockBalanceJpaEntity b
            WHERE b.productId = :productId
              AND b.branchId = :branchId
            """)
    Optional<StockBalanceJpaEntity> findForUpdate(@Param("productId") Long productId,
            @Param("branchId") Long branchId);

    // Búsqueda paginada del saldo con nombres de producto/sede. branchId null =
    // todas; q filtra
    // nombre/código;
    // lowStock=true → solo bajo mínimo. Solo productos ACTIVOS. Nativa (join
    // cross-feature por id
    // plano).
    // filterByProducts=false apaga el filtro por ids: el SQL nativo no admite un
    // IN vacio, asi que la bandera decide y la lista solo se lee cuando es true.
    // Ese filtro se SUMA al company_id del WHERE, nunca lo reemplaza.
    @Query(value = """
            SELECT sb.product_id AS productId, p.name AS productName, p.code AS productCode,
                   sb.branch_id AS branchId, b.name AS branchName, sb.quantity AS quantity,
                   sb.min_stock AS minStock
            FROM stock_balance sb
            JOIN products p ON p.id = sb.product_id
            JOIN branches b ON b.id = sb.branch_id
            WHERE sb.company_id = :companyId
              AND p.enabled = true
              AND (:branchId IS NULL OR sb.branch_id = :branchId)
              AND (:q IS NULL OR p.name LIKE CONCAT('%', :q, '%') OR p.code LIKE CONCAT('%', :q, '%'))
              AND (:lowStock = false OR sb.quantity < sb.min_stock)
              AND (:filterByProducts = false OR sb.product_id IN :productIds)
            ORDER BY p.name ASC, b.name ASC
            """, countQuery = """
            SELECT COUNT(*)
            FROM stock_balance sb
            JOIN products p ON p.id = sb.product_id
            WHERE sb.company_id = :companyId
              AND p.enabled = true
              AND (:branchId IS NULL OR sb.branch_id = :branchId)
              AND (:q IS NULL OR p.name LIKE CONCAT('%', :q, '%') OR p.code LIKE CONCAT('%', :q, '%'))
              AND (:lowStock = false OR sb.quantity < sb.min_stock)
              AND (:filterByProducts = false OR sb.product_id IN :productIds)
            """, nativeQuery = true)
    Page<StockRow> searchStock(@Param("companyId") Long companyId, @Param("branchId") Long branchId,
            @Param("q") String q, @Param("lowStock") boolean lowStock,
            @Param("filterByProducts") boolean filterByProducts,
            @Param("productIds") java.util.List<Long> productIds, Pageable pageable);

    // Bajo mínimo (para alertas): no paginado, solo productos activos con quantity
    // < min_stock.
    @Query(value = """
            SELECT sb.product_id AS productId, p.name AS productName, p.code AS productCode,
                   sb.branch_id AS branchId, b.name AS branchName, sb.quantity AS quantity,
                   sb.min_stock AS minStock
            FROM stock_balance sb
            JOIN products p ON p.id = sb.product_id
            JOIN branches b ON b.id = sb.branch_id
            WHERE sb.company_id = :companyId
              AND p.enabled = true
              AND (:branchId IS NULL OR sb.branch_id = :branchId)
              AND sb.quantity < sb.min_stock
            ORDER BY (sb.min_stock - sb.quantity) DESC, p.name ASC
            """, nativeQuery = true)
    java.util.List<StockRow> lowStockRows(@Param("companyId") Long companyId,
            @Param("branchId") Long branchId);

    @Query(value = """
            SELECT COUNT(*)
            FROM stock_balance sb
            JOIN products p ON p.id = sb.product_id
            WHERE p.enabled = true
              AND sb.quantity < sb.min_stock
            """, nativeQuery = true)
    long countLowStock();
}
