package com.vetsoftware.app.goodsreceipt.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GoodsReceiptJpaRepository
    extends JpaRepository<GoodsReceiptJpaEntity, Long>,
        JpaSpecificationExecutor<GoodsReceiptJpaEntity> {

  @Override
  @EntityGraph(attributePaths = {"company", "branch", "supplier", "lines", "lines.product"})
  Optional<GoodsReceiptJpaEntity> findById(Long id);

  @EntityGraph(attributePaths = {"company", "branch", "supplier", "lines", "lines.product"})
  Optional<GoodsReceiptJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

  @EntityGraph(attributePaths = {"company", "branch", "supplier", "lines", "lines.product"})
  List<GoodsReceiptJpaEntity> findAllByCompany_IdOrderByReceiptDateDescIdDesc(Long companyId);
}
