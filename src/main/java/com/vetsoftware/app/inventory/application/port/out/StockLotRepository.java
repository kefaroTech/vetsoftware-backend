package com.vetsoftware.app.inventory.application.port.out;

import com.vetsoftware.app.inventory.domain.StockLot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockLotRepository {
    StockLot save(StockLot lot);

    Optional<StockLot> findById(Long id);

    /**
     * Lotes de {@code (producto, sede)} disponibles ordenados FEFO: vencimiento más próximo primero
     * (nulls al final), luego por id. Solo lotes vigentes (enabled) con existencia > 0.
     */
    List<StockLot> findAvailableFefo(Long productId, Long branchId);

    /** Lote con identidad exacta (para acumular una entrada del mismo lote/costo). Maneja nulls en lote/vencimiento. */
    Optional<StockLot> findByIdentity(Long companyId, Long branchId, Long productId, String lotNumber,
                                      LocalDate expireDate, BigDecimal unitCost);
}
