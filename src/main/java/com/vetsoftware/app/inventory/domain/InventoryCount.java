package com.vetsoftware.app.inventory.domain;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sesión de conteo físico (cíclico) de inventario en una sede. Agrupa las
 * líneas contadas por producto; al confirmarse, cada línea con
 * {@link InventoryCountLine#difference()} distinta de cero genera un ajuste en
 * el ledger. Es el rastro de auditoría "qué se contó, quién y cuándo"; el
 * efecto sobre el stock vive en el kardex (append-only).
 */
public class InventoryCount {
    private Long id;
    private final Long companyId;
    private final Long branchId;
    private final String note;
    private final Long countedBy;
    private final LocalDateTime createdDate;
    private boolean enabled;
    private final List<InventoryCountLine> lines;

    public InventoryCount(Long id, Long companyId, Long branchId, String note, Long countedBy,
            LocalDateTime createdDate, boolean enabled, List<InventoryCountLine> lines) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (branchId == null)
            throw new IllegalArgumentException("branchId is required");
        if (lines == null || lines.isEmpty())
            throw new IllegalArgumentException("un conteo requiere al menos una línea");
        Set<Long> seen = new HashSet<>();
        for (InventoryCountLine line : lines) {
            if (!seen.add(line.getProductId()))
                throw new IllegalArgumentException(
                        "producto repetido en el conteo: " + line.getProductId());
        }
        this.id = id;
        this.companyId = companyId;
        this.branchId = branchId;
        this.note = note;
        this.countedBy = countedBy;
        this.createdDate = createdDate;
        this.enabled = enabled;
        this.lines = List.copyOf(lines);
    }

    public static InventoryCount create(Long companyId, Long branchId, String note, Long countedBy,
            List<InventoryCountLine> lines) {
        return new InventoryCount(null, companyId, branchId, note, countedBy, LocalDateTime.now(),
                true, lines);
    }

    public int totalLines() {
        return lines.size();
    }

    /** Cantidad de líneas cuya diferencia genera ajuste (≠ 0). */
    public int adjustedLines() {
        return (int) lines.stream().filter(l -> l.difference() != 0).count();
    }

    public Long getId() {
        return id;
    }

    public void assignId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getBranchId() {
        return branchId;
    }

    public String getNote() {
        return note;
    }

    public Long getCountedBy() {
        return countedBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void disable() {
        this.enabled = false;
    }

    public List<InventoryCountLine> getLines() {
        return lines;
    }
}
