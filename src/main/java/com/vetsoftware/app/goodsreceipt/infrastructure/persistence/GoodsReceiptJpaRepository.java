package com.vetsoftware.app.goodsreceipt.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface GoodsReceiptJpaRepository
        extends
            JpaRepository<GoodsReceiptJpaEntity, Long>,
            JpaSpecificationExecutor<GoodsReceiptJpaEntity> {

    @Override
    @EntityGraph(attributePaths = {"company", "branch", "supplier", "lines", "lines.product"})
    Optional<GoodsReceiptJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"company", "branch", "supplier", "lines", "lines.product"})
    Optional<GoodsReceiptJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = {"company", "branch", "supplier", "lines", "lines.product"})
    List<GoodsReceiptJpaEntity> findAllByCompany_IdOrderByReceiptDateDescIdDesc(Long companyId);

    // Baja lógica por query nativa, NUNCA por em.remove(). El @SQLDelete de la
    // entidad solo sustituye el DELETE de la raíz: el cascade a
    // goods_receipt_lines lo emite Hibernate antes y sin interceptar, así que
    // deleteById() dejaba la cabecera deshabilitada y el detalle borrado de la
    // base. Mismo choque que documenta AppointmentJpaRepository.softDelete.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE goods_receipts
            SET enabled = false
            WHERE id = :id
            """, nativeQuery = true)
    int softDelete(@Param("id") Long id);
}
