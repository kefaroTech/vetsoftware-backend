package com.vetsoftware.app.inventory.application.dto;

import com.vetsoftware.app.inventory.domain.InventoryCount;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Salida de una sesión de conteo. {@code lines} viene poblada en el detalle y al confirmar (para ver las diferencias
 * aplicadas); en el listado va vacía y solo se usan {@code totalLines}/{@code adjustedLines} (resumen).
 */
public record InventoryCountView(Long id, Long branchId, String note, Long countedBy, LocalDateTime createdDate,
                                 int totalLines, int adjustedLines, List<InventoryCountLineView> lines) {

    /** Vista completa (detalle / respuesta al confirmar) con las líneas y sus diferencias. */
    public static InventoryCountView from(InventoryCount c) {
        return new InventoryCountView(c.getId(), c.getBranchId(), c.getNote(), c.getCountedBy(), c.getCreatedDate(),
            c.totalLines(), c.adjustedLines(), c.getLines().stream().map(InventoryCountLineView::from).toList());
    }

    /** Vista de resumen (listado): sin líneas, solo los contadores agregados. */
    public static InventoryCountView summary(Long id, Long branchId, String note, Long countedBy,
                                             LocalDateTime createdDate, int totalLines, int adjustedLines) {
        return new InventoryCountView(id, branchId, note, countedBy, createdDate, totalLines, adjustedLines,
            List.of());
    }
}
