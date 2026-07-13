package com.vetsoftware.app.inventory.infrastructure.persistence;

import com.vetsoftware.app.inventory.application.port.out.StockLotRepository;
import com.vetsoftware.app.inventory.domain.StockLot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaStockLotRepository implements StockLotRepository {

    private final StockLotJpaRepository jpaRepository;

    public JpaStockLotRepository(StockLotJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public StockLot save(StockLot lot) {
        return toDomain(jpaRepository.save(toJpa(lot)));
    }

    @Override
    public Optional<StockLot> findById(Long id) {
        return jpaRepository.findById(id).map(JpaStockLotRepository::toDomain);
    }

    @Override
    public List<StockLot> findAvailableFefo(Long productId, Long branchId) {
        return jpaRepository.findAvailableFefo(productId, branchId).stream()
            .map(JpaStockLotRepository::toDomain).toList();
    }

    @Override
    public Optional<StockLot> findByIdentity(Long companyId, Long branchId, Long productId, String lotNumber,
                                             LocalDate expireDate, BigDecimal unitCost) {
        return jpaRepository.findByIdentity(companyId, branchId, productId, lotNumber, expireDate, unitCost)
            .stream().findFirst().map(JpaStockLotRepository::toDomain);
    }

    private static StockLotJpaEntity toJpa(StockLot lot) {
        StockLotJpaEntity e = new StockLotJpaEntity();
        e.setId(lot.getId());
        e.setCompanyId(lot.getCompanyId());
        e.setBranchId(lot.getBranchId());
        e.setProductId(lot.getProductId());
        e.setLotNumber(lot.getLotNumber());
        e.setExpireDate(lot.getExpireDate());
        e.setQuantityAvailable(lot.getQuantityAvailable());
        e.setUnitCost(lot.getUnitCost());
        e.setReceivedDate(lot.getReceivedDate());
        e.setCreatedDate(lot.getCreatedDate());
        e.setEnabled(lot.isEnabled());
        return e;
    }

    private static StockLot toDomain(StockLotJpaEntity e) {
        return new StockLot(e.getId(), e.getCompanyId(), e.getBranchId(), e.getProductId(), e.getLotNumber(),
            e.getExpireDate(), e.getQuantityAvailable(), e.getUnitCost(), e.getReceivedDate(),
            e.getCreatedDate(), e.isEnabled());
    }
}
