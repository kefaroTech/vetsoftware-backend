package com.vetsoftware.app.inventory.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.SQLRestriction;

/** Sesión de conteo físico. {@code total_lines}/{@code adjusted_lines} desnormalizados para el listado sin cargar líneas. */
@Entity
@Table(name = "inventory_count")
@SQLRestriction("enabled = true")
public class InventoryCountJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "note")
    private String note;

    @Column(name = "counted_by")
    private Long countedBy;

    @Column(name = "total_lines", nullable = false)
    private int totalLines;

    @Column(name = "adjusted_lines", nullable = false)
    private int adjustedLines;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @OneToMany(mappedBy = "count", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventoryCountLineJpaEntity> lines = new ArrayList<>();

    protected InventoryCountJpaEntity() {}

    public void addLine(InventoryCountLineJpaEntity line) {
        line.setCount(this);
        lines.add(line);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Long getCountedBy() { return countedBy; }
    public void setCountedBy(Long countedBy) { this.countedBy = countedBy; }
    public int getTotalLines() { return totalLines; }
    public void setTotalLines(int totalLines) { this.totalLines = totalLines; }
    public int getAdjustedLines() { return adjustedLines; }
    public void setAdjustedLines(int adjustedLines) { this.adjustedLines = adjustedLines; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public List<InventoryCountLineJpaEntity> getLines() { return lines; }
}
