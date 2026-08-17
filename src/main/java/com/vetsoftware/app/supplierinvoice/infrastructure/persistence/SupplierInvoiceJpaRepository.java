package com.vetsoftware.app.supplierinvoice.infrastructure.persistence;

import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SupplierInvoiceJpaRepository
        extends
            JpaRepository<SupplierInvoiceJpaEntity, Long>,
            JpaSpecificationExecutor<SupplierInvoiceJpaEntity> {

    @Override
    @EntityGraph(attributePaths = {"company", "branch", "supplier", "payments"})
    Optional<SupplierInvoiceJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"company", "branch", "supplier", "payments"})
    Optional<SupplierInvoiceJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = {"company", "branch", "supplier", "payments"})
    List<SupplierInvoiceJpaEntity> findAllByCompany_IdAndStatusInOrderByDueDateAsc(Long companyId,
            List<SupplierInvoiceStatus> statuses);

    @EntityGraph(attributePaths = {"company", "branch", "supplier", "payments"})
    List<SupplierInvoiceJpaEntity> findAllByCompany_IdAndBranch_IdAndStatusInOrderByDueDateAsc(
            Long companyId, Long branchId, List<SupplierInvoiceStatus> statuses);

    boolean existsByCompany_IdAndSupplier_IdAndInvoiceNumber(Long companyId, Long supplierId,
            String invoiceNumber);

    // Libro de compras (F4): facturas emitidas en el rango, excluyendo las
    // anuladas. Con abonos
    // cargados para
    // derivar pagado/saldo. Cross-feature: lo consume el adapter de purchasereport
    // (lectura de
    // persistencia).
    @EntityGraph(attributePaths = {"company", "branch", "supplier", "payments"})
    List<SupplierInvoiceJpaEntity> findAllByCompany_IdAndIssueDateBetweenAndStatusNotOrderByIssueDateAscIdAsc(
            Long companyId, java.time.LocalDate from, java.time.LocalDate to,
            SupplierInvoiceStatus status);

    @EntityGraph(attributePaths = {"company", "branch", "supplier", "payments"})
    List<SupplierInvoiceJpaEntity> findAllByCompany_IdAndBranch_IdAndIssueDateBetweenAndStatusNotOrderByIssueDateAscIdAsc(
            Long companyId, Long branchId, java.time.LocalDate from, java.time.LocalDate to,
            SupplierInvoiceStatus status);

    // Baja lógica por query nativa, NUNCA por em.remove(). El @SQLDelete de la
    // entidad solo sustituye el DELETE de la raíz: el cascade a
    // supplier_invoice_payments lo emite Hibernate antes y sin interceptar, así
    // que deleteById() dejaba la factura deshabilitada y sus abonos borrados de la
    // base. Mismo choque que documenta AppointmentJpaRepository.softDelete.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE supplier_invoices
            SET enabled = false
            WHERE id = :id
            """, nativeQuery = true)
    int softDelete(@Param("id") Long id);
}
