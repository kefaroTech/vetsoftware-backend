package com.vetsoftware.app.inventory.infrastructure.persistence;

import com.vetsoftware.app.inventory.application.port.out.StockBalanceRepository;
import com.vetsoftware.app.inventory.domain.StockBalance;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaStockBalanceRepository implements StockBalanceRepository {

    private final StockBalanceJpaRepository jpaRepository;

    public JpaStockBalanceRepository(StockBalanceJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public StockBalance save(StockBalance balance) {
        return toDomain(jpaRepository.save(toJpa(balance)));
    }

    @Override
    public Optional<StockBalance> find(Long productId, Long branchId) {
        return jpaRepository.findByProductIdAndBranchId(productId, branchId).map(JpaStockBalanceRepository::toDomain);
    }

    @Override
    public Optional<StockBalance> findForUpdate(Long productId, Long branchId) {
        return jpaRepository.findForUpdate(productId, branchId).map(JpaStockBalanceRepository::toDomain);
    }

    private static StockBalanceJpaEntity toJpa(StockBalance b) {
        StockBalanceJpaEntity e = new StockBalanceJpaEntity();
        e.setId(b.getId());
        e.setCompanyId(b.getCompanyId());
        e.setBranchId(b.getBranchId());
        e.setProductId(b.getProductId());
        e.setQuantity(b.getQuantity());
        e.setMinStock(b.getMinStock());
        e.setVersion(b.getVersion());
        return e;
    }

    private static StockBalance toDomain(StockBalanceJpaEntity e) {
        return new StockBalance(e.getId(), e.getCompanyId(), e.getBranchId(), e.getProductId(),
            e.getQuantity(), e.getMinStock(), e.getVersion());
    }
}
