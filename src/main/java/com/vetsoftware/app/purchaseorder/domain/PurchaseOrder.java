package com.vetsoftware.app.purchaseorder.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Orden de compra = pedido de productos a un proveedor para una sede. Agregado
 * company-scoped con soft-delete y bloqueo optimista (@Version). Su ciclo de
 * vida lo gobierna {@link PurchaseOrderStatus}: se crea en DRAFT (editable), se
 * emite con {@link #place}, se recibe línea a línea (mueve a
 * PARTIALLY_RECEIVED/RECEIVED) y puede cancelarse mientras no se haya recibido
 * nada.
 */
public class PurchaseOrder {
    private Long id;
    private CompanyRef company;
    private BranchRef branch;
    private SupplierRef supplier;
    private PurchaseOrderStatus status;
    private LocalDate orderDate;
    private LocalDate expectedDate;
    private String notes;
    private List<PurchaseOrderLine> lines;
    private final LocalDateTime createdDate;
    private final Long createdBy;
    private LocalDateTime updatedDate;
    private Long updatedBy;
    private Long version;
    private boolean enabled;

    public PurchaseOrder(Long id, CompanyRef company, BranchRef branch, SupplierRef supplier,
            PurchaseOrderStatus status, LocalDate orderDate, LocalDate expectedDate, String notes,
            List<PurchaseOrderLine> lines, LocalDateTime createdDate, Long createdBy,
            LocalDateTime updatedDate, Long updatedBy, Long version, boolean enabled) {
        validate(company, branch, supplier, status, orderDate, notes, lines);
        this.id = id;
        this.company = company;
        this.branch = branch;
        this.supplier = supplier;
        this.status = status;
        this.orderDate = orderDate;
        this.expectedDate = expectedDate;
        this.notes = notes;
        this.lines = new ArrayList<>(lines);
        this.createdDate = createdDate;
        this.createdBy = createdBy;
        this.updatedDate = updatedDate;
        this.updatedBy = updatedBy;
        this.version = version;
        this.enabled = enabled;
    }

    public static PurchaseOrder create(CompanyRef company, BranchRef branch, SupplierRef supplier,
            LocalDate orderDate, LocalDate expectedDate, String notes,
            List<PurchaseOrderLine> lines, Long createdBy) {
        return new PurchaseOrder(null, company, branch, supplier, PurchaseOrderStatus.DRAFT,
                orderDate, expectedDate, notes, lines, LocalDateTime.now(), createdBy, null, null,
                null, true);
    }

    /** Edita cabecera y líneas. Solo permitido en DRAFT. */
    public void update(BranchRef branch, SupplierRef supplier, LocalDate orderDate,
            LocalDate expectedDate, String notes, List<PurchaseOrderLine> lines, Long updatedBy,
            Long expectedVersion) {
        if (status != PurchaseOrderStatus.DRAFT) {
            throw new InvalidPurchaseOrderStatusTransitionException(
                    "Purchase order can only be edited while in DRAFT (current: " + status + ")");
        }
        validate(company, branch, supplier, status, orderDate, notes, lines);
        this.branch = branch;
        this.supplier = supplier;
        this.orderDate = orderDate;
        this.expectedDate = expectedDate;
        this.notes = notes;
        this.lines = new ArrayList<>(lines);
        this.updatedDate = LocalDateTime.now();
        this.updatedBy = updatedBy;
        this.version = expectedVersion;
    }

    /** DRAFT → PLACED (emite la orden al proveedor). */
    public void place(Long updatedBy) {
        if (status != PurchaseOrderStatus.DRAFT) {
            throw new InvalidPurchaseOrderStatusTransitionException(status,
                    PurchaseOrderStatus.PLACED);
        }
        this.status = PurchaseOrderStatus.PLACED;
        touch(updatedBy);
    }

    /** DRAFT/PLACED → CANCELLED. No se puede cancelar si ya hay algo recibido. */
    public void cancel(Long updatedBy) {
        if (status != PurchaseOrderStatus.DRAFT && status != PurchaseOrderStatus.PLACED) {
            throw new InvalidPurchaseOrderStatusTransitionException(status,
                    PurchaseOrderStatus.CANCELLED);
        }
        if (lines.stream().anyMatch(l -> l.getQuantityReceived() > 0)) {
            throw new InvalidPurchaseOrderStatusTransitionException(
                    "Cannot cancel a purchase order that already has received items");
        }
        this.status = PurchaseOrderStatus.CANCELLED;
        touch(updatedBy);
    }

    /**
     * Aplica lo recibido a una línea concreta (usado por la recepción de
     * mercancía).
     */
    public void receiveLine(Long purchaseOrderLineId, int quantity) {
        lineById(purchaseOrderLineId).receive(quantity);
    }

    /** Reversa lo recibido de una línea concreta (reversa de una recepción). */
    public void revertLine(Long purchaseOrderLineId, int quantity) {
        lineById(purchaseOrderLineId).revertReceive(quantity);
    }

    /**
     * Recalcula el estado tras aplicar recepciones: todo recibido → RECEIVED; algo
     * recibido → PARTIALLY_RECEIVED.
     */
    public void recalculateStatusAfterReceipt(Long updatedBy) {
        if (lines.stream().allMatch(PurchaseOrderLine::isFullyReceived)) {
            this.status = PurchaseOrderStatus.RECEIVED;
        } else if (lines.stream().anyMatch(l -> l.getQuantityReceived() > 0)) {
            this.status = PurchaseOrderStatus.PARTIALLY_RECEIVED;
        }
        touch(updatedBy);
    }

    /**
     * Recalcula el estado tras revertir recepciones: algo recibido →
     * PARTIALLY_RECEIVED; nada → PLACED.
     */
    public void recalculateStatusAfterRevert(Long updatedBy) {
        if (lines.stream().anyMatch(l -> l.getQuantityReceived() > 0)) {
            this.status = PurchaseOrderStatus.PARTIALLY_RECEIVED;
        } else {
            this.status = PurchaseOrderStatus.PLACED;
        }
        touch(updatedBy);
    }

    private PurchaseOrderLine lineById(Long purchaseOrderLineId) {
        if (purchaseOrderLineId == null)
            throw new IllegalArgumentException("purchase order line id is required");
        return lines.stream().filter(l -> purchaseOrderLineId.equals(l.getId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Purchase order line "
                        + purchaseOrderLineId + " not found in order " + id));
    }

    private void touch(Long updatedBy) {
        this.updatedDate = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }

    private static void validate(CompanyRef company, BranchRef branch, SupplierRef supplier,
            PurchaseOrderStatus status, LocalDate orderDate, String notes,
            List<PurchaseOrderLine> lines) {
        if (company == null)
            throw new IllegalArgumentException("company is required");
        if (branch == null)
            throw new IllegalArgumentException("branch is required");
        if (supplier == null)
            throw new IllegalArgumentException("supplier is required");
        if (status == null)
            throw new IllegalArgumentException("status is required");
        if (orderDate == null)
            throw new IllegalArgumentException("orderDate is required");
        if (notes != null && notes.length() > 500)
            throw new IllegalArgumentException("notes must be 500 chars or less");
        if (lines == null || lines.isEmpty())
            throw new IllegalArgumentException("purchase order must have at least one line");
        Set<Long> productIds = new HashSet<>();
        for (PurchaseOrderLine line : lines) {
            if (line == null)
                throw new IllegalArgumentException("purchase order line is required");
            if (!productIds.add(line.getProduct().id())) {
                throw new IllegalArgumentException(
                        "duplicate product in purchase order lines: " + line.getProduct().id());
            }
        }
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

    public PurchaseOrderStatus getStatus() {
        return status;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public LocalDate getExpectedDate() {
        return expectedDate;
    }

    public String getNotes() {
        return notes;
    }

    public List<PurchaseOrderLine> getLines() {
        return List.copyOf(lines);
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
