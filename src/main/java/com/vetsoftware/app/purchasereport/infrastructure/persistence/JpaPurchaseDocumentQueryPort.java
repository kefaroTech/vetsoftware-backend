package com.vetsoftware.app.purchasereport.infrastructure.persistence;

import com.vetsoftware.app.purchasereport.application.port.out.PurchaseDocumentQueryPort;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceStatus;
import com.vetsoftware.app.supplierinvoice.infrastructure.persistence.SupplierInvoiceJpaEntity;
import com.vetsoftware.app.supplierinvoice.infrastructure.persistence.SupplierInvoiceJpaRepository;
import com.vetsoftware.app.supplierinvoice.infrastructure.persistence.SupplierInvoicePaymentJpaEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Lee las facturas de proveedor de la empresa (read-only) para el libro de
 * compras (F4). Cruce permitido de vertical slicing: la infraestructura de
 * {@code purchasereport} importa el JpaRepository/JpaEntity de
 * {@code supplierinvoice}. Deriva pagado/saldo desde los abonos y expone el
 * estado como String en el borde.
 */
@Component
public class JpaPurchaseDocumentQueryPort implements PurchaseDocumentQueryPort {

    private final SupplierInvoiceJpaRepository invoiceJpaRepository;

    public JpaPurchaseDocumentQueryPort(SupplierInvoiceJpaRepository invoiceJpaRepository) {
        this.invoiceJpaRepository = invoiceJpaRepository;
    }

    @Override
    public List<PurchaseDocumentView> findByCompanyAndDateRange(Long companyId, LocalDate from,
            LocalDate to, Long branchId) {
        List<SupplierInvoiceJpaEntity> invoices = branchId == null
                ? invoiceJpaRepository
                        .findAllByCompany_IdAndIssueDateBetweenAndStatusNotOrderByIssueDateAscIdAsc(
                                companyId, from, to, SupplierInvoiceStatus.CANCELLED)
                : invoiceJpaRepository
                        .findAllByCompany_IdAndBranch_IdAndIssueDateBetweenAndStatusNotOrderByIssueDateAscIdAsc(
                                companyId, branchId, from, to, SupplierInvoiceStatus.CANCELLED);
        return invoices.stream().map(this::toView).toList();
    }

    private PurchaseDocumentView toView(SupplierInvoiceJpaEntity e) {
        BigDecimal paid = e.getPayments().stream().map(SupplierInvoicePaymentJpaEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payable = e.getTotal().subtract(e.getWithholdingAmount());
        BigDecimal balance = payable.subtract(paid);
        return new PurchaseDocumentView(e.getId(), e.getSupplier().getName(),
                e.getSupplier().getTaxId(), e.getInvoiceNumber(), e.getIssueDate(), e.getDueDate(),
                e.getSubtotal(), e.getTaxAmount(), e.getWithholdingAmount(), e.getTotal(), paid,
                balance, e.getStatus() == null ? null : e.getStatus().name());
    }
}
