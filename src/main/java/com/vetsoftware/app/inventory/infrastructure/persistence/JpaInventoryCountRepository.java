package com.vetsoftware.app.inventory.infrastructure.persistence;

import com.vetsoftware.app.inventory.application.command.SearchCountsQuery;
import com.vetsoftware.app.inventory.application.dto.InventoryCountView;
import com.vetsoftware.app.inventory.application.dto.PageResult;
import com.vetsoftware.app.inventory.application.port.out.InventoryCountRepository;
import com.vetsoftware.app.inventory.domain.InventoryCount;
import com.vetsoftware.app.inventory.domain.InventoryCountLine;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class JpaInventoryCountRepository implements InventoryCountRepository {

    private final InventoryCountJpaRepository jpaRepository;

    public JpaInventoryCountRepository(InventoryCountJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public InventoryCount save(InventoryCount count) {
        InventoryCountJpaEntity entity = new InventoryCountJpaEntity();
        entity.setCompanyId(count.getCompanyId());
        entity.setBranchId(count.getBranchId());
        entity.setNote(count.getNote());
        entity.setCountedBy(count.getCountedBy());
        entity.setTotalLines(count.totalLines());
        entity.setAdjustedLines(count.adjustedLines());
        entity.setCreatedDate(count.getCreatedDate());
        entity.setEnabled(count.isEnabled());
        for (InventoryCountLine line : count.getLines()) {
            InventoryCountLineJpaEntity lineEntity = new InventoryCountLineJpaEntity();
            lineEntity.setProductId(line.getProductId());
            lineEntity.setSystemQuantity(line.getSystemQuantity());
            lineEntity.setCountedQuantity(line.getCountedQuantity());
            lineEntity.setDifference(line.difference());
            entity.addLine(lineEntity);
        }
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public PageResult<InventoryCountView> search(SearchCountsQuery query) {
        Page<InventoryCountJpaEntity> page = jpaRepository.search(query.companyId(), query.branchId(),
            PageRequest.of(query.page(), query.pageSize()));
        List<InventoryCountView> content = page.getContent().stream()
            .map(e -> InventoryCountView.summary(e.getId(), e.getBranchId(), e.getNote(), e.getCountedBy(),
                e.getCreatedDate(), e.getTotalLines(), e.getAdjustedLines()))
            .toList();
        return new PageResult<>(content, page.getNumber(), page.getSize(), page.getTotalElements(),
            page.getTotalPages());
    }

    @Override
    public Optional<InventoryCountView> findDetail(Long companyId, Long id) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(e -> InventoryCountView.from(toDomain(e)));
    }

    private static InventoryCount toDomain(InventoryCountJpaEntity e) {
        List<InventoryCountLine> lines = e.getLines().stream()
            .map(l -> new InventoryCountLine(l.getId(), l.getProductId(), l.getSystemQuantity(),
                l.getCountedQuantity()))
            .toList();
        return new InventoryCount(e.getId(), e.getCompanyId(), e.getBranchId(), e.getNote(), e.getCountedBy(),
            e.getCreatedDate(), e.isEnabled(), lines);
    }
}
