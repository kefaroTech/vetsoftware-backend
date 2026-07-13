package com.vetsoftware.app.inventory.application.port.out;

import com.vetsoftware.app.inventory.domain.StockBalance;
import java.util.Optional;

public interface StockBalanceRepository {
    StockBalance save(StockBalance balance);

    Optional<StockBalance> find(Long productId, Long branchId);

    /**
     * Igual que {@link #find} pero con lock pesimista ({@code SELECT ... FOR UPDATE}): serializa las salidas
     * concurrentes del mismo producto en la misma sede para no sobrevender.
     */
    Optional<StockBalance> findForUpdate(Long productId, Long branchId);
}
