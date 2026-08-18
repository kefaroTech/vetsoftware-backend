package com.vetsoftware.app.supplierinvoice.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Factura de proveedor = documento de compra recibido de un tercero + su cuenta
 * por pagar (agregado company-scoped imputado a una sede). Nace en PENDING; los
 * abonos (append-only) la mueven a PARTIAL/PAID; puede anularse solo mientras
 * no tenga abonos. Los montos fiscales (base/impuesto/retención) se capturan a
 * nivel cabecera para alimentar el libro de compras (F4); el total bruto se
 * calcula internamente = base + impuesto.
 *
 * <p>
 * Semántica de dinero:
 *
 * <ul>
 * <li>{@code total} (bruto) = {@code subtotal} + {@code taxAmount}.
 * <li>{@code payableAmount} (lo que se le paga al proveedor) = {@code total} −
 * {@code
 *       withholdingAmount}.
 * <li>{@code balance} (saldo por pagar) = {@code payableAmount} −
 * {@code paidAmount}.
 * </ul>
 */
public class SupplierInvoice {
    private Long id;
    private CompanyRef company;
    private BranchRef branch;
    private SupplierRef supplier;
    private Long purchaseOrderId;
    private Long goodsReceiptId;
    private String invoiceNumber;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal withholdingAmount;
    private BigDecimal total;
    private SupplierInvoiceStatus status;
    private String notes;
    private List<SupplierInvoicePayment> payments;
    private final LocalDateTime createdDate;
    private final Long createdBy;
    private LocalDateTime updatedDate;
    private Long updatedBy;
    private Long version;
    private boolean enabled;

    public SupplierInvoice(Long id, CompanyRef company, BranchRef branch, SupplierRef supplier,
            Long purchaseOrderId, Long goodsReceiptId, String invoiceNumber, LocalDate issueDate,
            LocalDate dueDate, BigDecimal subtotal, BigDecimal taxAmount,
            BigDecimal withholdingAmount, SupplierInvoiceStatus status, String notes,
            List<SupplierInvoicePayment> payments, LocalDateTime createdDate, Long createdBy,
            LocalDateTime updatedDate, Long updatedBy, Long version, boolean enabled) {
        validate(company, branch, supplier, invoiceNumber, issueDate, dueDate, subtotal, taxAmount,
                withholdingAmount, status, notes);
        this.id = id;
        this.company = company;
        this.branch = branch;
        this.supplier = supplier;
        this.purchaseOrderId = purchaseOrderId;
        this.goodsReceiptId = goodsReceiptId;
        this.invoiceNumber = invoiceNumber;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.withholdingAmount = withholdingAmount;
        this.total = subtotal.add(taxAmount);
        this.status = status;
        this.notes = notes;
        this.payments = new ArrayList<>(payments);
        this.createdDate = createdDate;
        this.createdBy = createdBy;
        this.updatedDate = updatedDate;
        this.updatedBy = updatedBy;
        this.version = version;
        this.enabled = enabled;
    }

    public static SupplierInvoice create(CompanyRef company, BranchRef branch, SupplierRef supplier,
            Long purchaseOrderId, Long goodsReceiptId, String invoiceNumber, LocalDate issueDate,
            LocalDate dueDate, BigDecimal subtotal, BigDecimal taxAmount,
            BigDecimal withholdingAmount, String notes, Long createdBy) {
        return new SupplierInvoice(null, company, branch, supplier, purchaseOrderId, goodsReceiptId,
                invoiceNumber, issueDate, dueDate, subtotal, taxAmount, withholdingAmount,
                SupplierInvoiceStatus.PENDING, notes, new ArrayList<>(), LocalDateTime.now(),
                createdBy, null, null, null, true);
    }

    /**
     * Edición general: solo permitida mientras no tenga abonos (status PENDING).
     */
    public void update(BranchRef branch, SupplierRef supplier, Long purchaseOrderId,
            Long goodsReceiptId, String invoiceNumber, LocalDate issueDate, LocalDate dueDate,
            BigDecimal subtotal, BigDecimal taxAmount, BigDecimal withholdingAmount, String notes,
            Long updatedBy, Long expectedVersion) {
        if (status != SupplierInvoiceStatus.PENDING) {
            throw new InvalidSupplierInvoiceStateException(
                    "Supplier invoice can only be edited while PENDING (no payments), current status: "
                            + status);
        }
        validate(company, branch, supplier, invoiceNumber, issueDate, dueDate, subtotal, taxAmount,
                withholdingAmount, status, notes);
        this.branch = branch;
        this.supplier = supplier;
        this.purchaseOrderId = purchaseOrderId;
        this.goodsReceiptId = goodsReceiptId;
        this.invoiceNumber = invoiceNumber;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.withholdingAmount = withholdingAmount;
        this.notes = notes;
        this.total = subtotal.add(taxAmount);
        this.updatedDate = LocalDateTime.now();
        this.updatedBy = updatedBy;
        this.version = expectedVersion;
    }

    /**
     * Registra un abono y recalcula el estado (PARTIAL/PAID). Rechaza abonos sobre
     * facturas anuladas/pagadas o sobrepagos.
     */
    public void registerPayment(SupplierInvoicePayment payment, Long actorId,
            Long expectedVersion) {
        if (status == SupplierInvoiceStatus.CANCELLED) {
            throw new InvalidSupplierInvoiceStateException("Cannot pay a cancelled invoice");
        }
        if (status == SupplierInvoiceStatus.PAID) {
            throw new InvalidSupplierInvoiceStateException("Invoice is already fully paid");
        }
        if (payment.getAmount().compareTo(balance()) > 0) {
            throw new InvalidSupplierInvoiceStateException(
                    "Payment " + payment.getAmount() + " exceeds outstanding balance " + balance());
        }
        this.payments.add(payment);
        this.status = paidAmount().compareTo(payableAmount()) >= 0
                ? SupplierInvoiceStatus.PAID
                : SupplierInvoiceStatus.PARTIAL;
        this.updatedDate = LocalDateTime.now();
        this.updatedBy = actorId;
        this.version = expectedVersion;
    }

    /** Anula la factura. Solo posible mientras no tenga abonos (PENDING). */
    public void cancel(Long actorId, Long expectedVersion) {
        if (status != SupplierInvoiceStatus.PENDING) {
            throw new InvalidSupplierInvoiceStateException(
                    "Only a PENDING invoice (without payments) can be cancelled, current status: "
                            + status);
        }
        this.status = SupplierInvoiceStatus.CANCELLED;
        this.updatedDate = LocalDateTime.now();
        this.updatedBy = actorId;
        this.version = expectedVersion;
    }

    /** Total bruto = base + impuesto. */
    public BigDecimal getTotal() {
        return total;
    }

    /** Lo que se le debe pagar al proveedor = total − retención practicada. */
    public BigDecimal payableAmount() {
        return total.subtract(withholdingAmount);
    }

    /** Suma de abonos aplicados. */
    public BigDecimal paidAmount() {
        return payments.stream().map(SupplierInvoicePayment::getAmount).reduce(BigDecimal.ZERO,
                BigDecimal::add);
    }

    /** Saldo pendiente por pagar (0 si está pagada/anulada). */
    public BigDecimal balance() {
        if (status == SupplierInvoiceStatus.CANCELLED)
            return BigDecimal.ZERO;
        return payableAmount().subtract(paidAmount());
    }

    private static void validate(CompanyRef company, BranchRef branch, SupplierRef supplier,
            String invoiceNumber, LocalDate issueDate, LocalDate dueDate, BigDecimal subtotal,
            BigDecimal taxAmount, BigDecimal withholdingAmount, SupplierInvoiceStatus status,
            String notes) {
        if (company == null)
            throw new IllegalArgumentException("company is required");
        if (branch == null)
            throw new IllegalArgumentException("branch is required");
        if (supplier == null)
            throw new IllegalArgumentException("supplier is required");
        if (invoiceNumber == null || invoiceNumber.isBlank())
            throw new IllegalArgumentException("invoiceNumber is required");
        if (invoiceNumber.length() > 60)
            throw new IllegalArgumentException("invoiceNumber must be 60 chars or less");
        if (issueDate == null)
            throw new IllegalArgumentException("issueDate is required");
        if (dueDate == null)
            throw new IllegalArgumentException("dueDate is required");
        if (dueDate.isBefore(issueDate))
            throw new IllegalArgumentException("dueDate cannot be before issueDate");
        if (subtotal == null || subtotal.signum() < 0)
            throw new IllegalArgumentException("subtotal cannot be negative");
        if (taxAmount == null || taxAmount.signum() < 0)
            throw new IllegalArgumentException("taxAmount cannot be negative");
        if (withholdingAmount == null || withholdingAmount.signum() < 0)
            throw new IllegalArgumentException("withholdingAmount cannot be negative");
        if (withholdingAmount.compareTo(subtotal.add(taxAmount)) > 0) {
            throw new IllegalArgumentException("withholdingAmount cannot exceed the gross total");
        }
        if (status == null)
            throw new IllegalArgumentException("status is required");
        if (notes != null && notes.length() > 500)
            throw new IllegalArgumentException("notes must be 500 chars or less");
    }

    public Long getId() {
        return id;
    }

    public CompanyRef getCompany() {
        return company;
    }

    public BranchRef getBranch() {
        return branch;
    }

    public SupplierRef getSupplier() {
        return supplier;
    }

    public Long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public Long getGoodsReceiptId() {
        return goodsReceiptId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getWithholdingAmount() {
        return withholdingAmount;
    }

    public SupplierInvoiceStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public List<SupplierInvoicePayment> getPayments() {
        return List.copyOf(payments);
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }
}
