package com.vetsoftware.app.inventory.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockLotJpaRepository extends JpaRepository<StockLotJpaEntity, Long> {

    // FEFO: vencimiento más próximo primero, nulls al final, luego por id. Solo
    // lotes vigentes con
    // existencia.
    @Query("SELECT l FROM StockLotJpaEntity l WHERE l.productId = :productId AND l.branchId = :branchId"
            + " AND l.enabled = true AND l.quantityAvailable > 0 ORDER BY CASE WHEN l.expireDate IS"
            + " NULL THEN 1 ELSE 0 END ASC, l.expireDate ASC, l.id ASC")
    List<StockLotJpaEntity> findAvailableFefo(@Param("productId") Long productId,
            @Param("branchId") Long branchId);

    // Identidad exacta de lote (para acumular la misma entrada). Maneja nulls en
    // lote/vencimiento.
    @Query("SELECT l FROM StockLotJpaEntity l WHERE l.companyId = :companyId AND l.branchId = :branchId "
            + "AND l.productId = :productId AND l.enabled = true AND l.unitCost = :unitCost "
            + "AND ((:lotNumber IS NULL AND l.lotNumber IS NULL) OR l.lotNumber = :lotNumber) "
            + "AND ((:expireDate IS NULL AND l.expireDate IS NULL) OR l.expireDate = :expireDate) "
            + "ORDER BY l.id ASC")
    List<StockLotJpaEntity> findByIdentity(@Param("companyId") Long companyId,
            @Param("branchId") Long branchId, @Param("productId") Long productId,
            @Param("lotNumber") String lotNumber, @Param("expireDate") LocalDate expireDate,
            @Param("unitCost") BigDecimal unitCost);

    // Vista de lotes con existencia (FEFO) para UI/trazabilidad. branchId null =
    // todas las sedes de
    // la empresa.
    @Query("SELECT l FROM StockLotJpaEntity l WHERE l.companyId = :companyId AND l.productId ="
            + " :productId AND (:branchId IS NULL OR l.branchId = :branchId) AND l.enabled = true AND"
            + " l.quantityAvailable > 0 ORDER BY CASE WHEN l.expireDate IS NULL THEN 1 ELSE 0 END"
            + " ASC, l.expireDate ASC, l.id ASC")
    List<StockLotJpaEntity> findLotsForView(@Param("companyId") Long companyId,
            @Param("productId") Long productId, @Param("branchId") Long branchId);

    // Lotes por vencer (o vencidos) con existencia, hasta la fecha límite.
    // daysToExpire negativo = ya
    // vencido.
    @Query(value = "SELECT l.id AS lotId, l.product_id AS productId, p.name AS productName, p.code AS"
            + " productCode, l.branch_id AS branchId, b.name AS branchName, l.lot_number AS"
            + " lotNumber, l.expire_date AS expireDate, l.quantity_available AS"
            + " quantityAvailable, DATEDIFF(l.expire_date, CURRENT_DATE) AS daysToExpire FROM"
            + " stock_lot l JOIN products p ON p.id = l.product_id JOIN branches b ON b.id ="
            + " l.branch_id WHERE l.company_id = :companyId AND l.enabled = true AND"
            + " l.quantity_available > 0 AND l.expire_date IS NOT NULL AND l.expire_date <="
            + " :limitDate AND (:branchId IS NULL OR l.branch_id = :branchId) ORDER BY"
            + " l.expire_date ASC", nativeQuery = true)
    List<ExpiringLotRow> expiringLots(@Param("companyId") Long companyId,
            @Param("branchId") Long branchId, @Param("limitDate") LocalDate limitDate);

    // Valuación agregada por producto: Σ unidades y Σ (unidades × costo) sobre los
    // lotes activos.
    @Query(value = "SELECT l.product_id AS productId, p.name AS productName, p.code AS productCode,"
            + " SUM(l.quantity_available) AS quantity, SUM(l.quantity_available * l.unit_cost) AS"
            + " value FROM stock_lot l JOIN products p ON p.id = l.product_id WHERE l.company_id"
            + " = :companyId AND l.enabled = true AND (:branchId IS NULL OR l.branch_id ="
            + " :branchId) GROUP BY l.product_id, p.name, p.code HAVING SUM(l.quantity_available)"
            + " <> 0 ORDER BY p.name ASC", nativeQuery = true)
    List<ValuationRow> valuationRows(@Param("companyId") Long companyId,
            @Param("branchId") Long branchId);

    @Query("SELECT COUNT(l) FROM StockLotJpaEntity l "
            + "WHERE l.enabled = true AND l.quantityAvailable > 0 "
            + "AND l.expireDate IS NOT NULL AND l.expireDate < :before")
    long countExpiredBefore(@Param("before") LocalDate before);

    @Query("SELECT COUNT(l) FROM StockLotJpaEntity l "
            + "WHERE l.enabled = true AND l.quantityAvailable > 0 "
            + "AND l.expireDate >= :from AND l.expireDate <= :to")
    long countExpiringBetweenInclusive(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COUNT(l) FROM StockLotJpaEntity l "
            + "WHERE l.enabled = true AND l.quantityAvailable > 0 "
            + "AND l.expireDate > :fromExclusive AND l.expireDate <= :to")
    long countExpiringAfterUntil(@Param("fromExclusive") LocalDate fromExclusive,
            @Param("to") LocalDate to);
}
