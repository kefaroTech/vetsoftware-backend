package com.vetsoftware.app.goodsreceipt.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Recepción de mercancía = ingreso físico de bienes de un proveedor a una sede
 * (con o sin orden de compra previa). Agregado company-scoped con líneas por
 * producto/lote. Se crea en DRAFT (editable); al CONFIRMAR afecta el inventario
 * (kardex por sede) y, si viene de una orden de compra, registra lo recibido en
 * ella; al CANCELAR se revierte todo. La transición de estado es la guarda de
 * idempotencia frente al ledger de inventario.
 */
public class GoodsReceipt {
    private Long id;
    private CompanyRef company;
    private BranchRef branch;
    private SupplierRef supplier;
    private Long purchaseOrderId;
    private LocalDate receiptDate;
    private String supplierInvoiceNumber;
    private String notes;
    private GoodsReceiptStatus status;
    private List<GoodsReceiptLine> lines;
    private final LocalDateTime createdDate;
    private final Long createdBy;
    private LocalDateTime updatedDate;
    private Long updatedBy;
    private Long version;
    private boolean enabled;

    public GoodsReceipt(Long id, CompanyRef company, BranchRef branch, SupplierRef supplier,
            Long purchaseOrderId, LocalDate receiptDate, String supplierInvoiceNumber, String notes,
            GoodsReceiptStatus status, List<GoodsReceiptLine> lines, LocalDateTime createdDate,
            Long createdBy, LocalDateTime updatedDate, Long updatedBy, Long version,
            boolean enabled) {
        validate(company, branch, supplier, receiptDate, supplierInvoiceNumber, notes, status,
                lines);
        this.id = id;
        this.company = company;
        this.branch = branch;
        this.supplier = supplier;
        this.purchaseOrderId = purchaseOrderId;
        this.receiptDate = receiptDate;
        this.supplierInvoiceNumber = supplierInvoiceNumber;
        this.notes = notes;
        this.status = status;
        this.lines = List.copyOf(lines);
        this.createdDate = createdDate;
        this.createdBy = createdBy;
        this.updatedDate = updatedDate;
        this.updatedBy = updatedBy;
        this.version = version;
        this.enabled = enabled;
    }

    public static GoodsReceipt create(CompanyRef company, BranchRef branch, SupplierRef supplier,
            Long purchaseOrderId, LocalDate receiptDate, String supplierInvoiceNumber, String notes,
            List<GoodsReceiptLine> lines, Long createdBy) {
        return new GoodsReceipt(null, company, branch, supplier, purchaseOrderId, receiptDate,
                supplierInvoiceNumber, notes, GoodsReceiptStatus.DRAFT, lines, LocalDateTime.now(),
                createdBy, null, null, null, true);
    }

    /** DRAFT → CONFIRMED. Guarda de idempotencia antes de tocar el inventario. */
    public void confirm(Long actorId) {
        if (status != GoodsReceiptStatus.DRAFT) {
            throw new InvalidGoodsReceiptStatusTransitionException(status,
                    GoodsReceiptStatus.CONFIRMED);
        }
        this.status = GoodsReceiptStatus.CONFIRMED;
        this.updatedDate = LocalDateTime.now();
        this.updatedBy = actorId;
    }

    /** CONFIRMED → CANCELLED. Revierte los movimientos de inventario asociados. */
    public void cancel(Long actorId) {
        if (status != GoodsReceiptStatus.CONFIRMED) {
            throw new InvalidGoodsReceiptStatusTransitionException(status,
                    GoodsReceiptStatus.CANCELLED);
        }
        this.status = GoodsReceiptStatus.CANCELLED;
        this.updatedDate = LocalDateTime.now();
        this.updatedBy = actorId;
    }

    private static void validate(CompanyRef company, BranchRef branch, SupplierRef supplier,
            LocalDate receiptDate, String supplierInvoiceNumber, String notes,
            GoodsReceiptStatus status, List<GoodsReceiptLine> lines) {
        if (company == null)
            throw new IllegalArgumentException("company is required");
        if (branch == null)
            throw new IllegalArgumentException("branch is required");
        if (supplier == null)
            throw new IllegalArgumentException("supplier is required");
        if (receiptDate == null)
            throw new IllegalArgumentException("receiptDate is required");
        if (supplierInvoiceNumber != null && supplierInvoiceNumber.length() > 60) {
            throw new IllegalArgumentException("supplierInvoiceNumber must be 60 chars or less");
        }
        if (notes != null && notes.length() > 500)
            throw new IllegalArgumentException("notes must be 500 chars or less");
        if (status == null)
            throw new IllegalArgumentException("status is required");
        if (lines == null || lines.isEmpty())
            throw new IllegalArgumentException("at least one line is required");
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

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public String getSupplierInvoiceNumber() {
        return supplierInvoiceNumber;
    }

    public String getNotes() {
        return notes;
    }

    public GoodsReceiptStatus getStatus() {
        return status;
    }

    public List<GoodsReceiptLine> getLines() {
        return lines;
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
