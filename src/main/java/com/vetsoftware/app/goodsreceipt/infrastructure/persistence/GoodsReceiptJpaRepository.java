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
    //
    // El AND company_id no es defensa en profundidad redundante con la lectura
    // previa del servicio: es la barrera que sobrevive a que alguien reordene el
    // caso de uso o llame al adaptador desde otro sitio. No hay sobrecarga ancha
    // porque no hay camino SYSTEM: el controller resuelve la empresa con
    // authz.currentCompanyId(), que ya rechaza al principal sin empresa.
    //
    // El UPDATE mueve tambien `version`, la del bloqueo optimista, a proposito:
    // una consulta nativa no la comprueba ni la incrementa, asi que un save
    // cargado antes de la baja reescribia la fila entera desde el dominio —el
    // mapper la copia— y su WHERE version = ? casaba igual, resucitando en
    // silencio la remision recien dada de baja. Movida la version, ese save ya
    // no encuentra fila y salta ObjectOptimisticLockingFailureException -> 409
    // CONCURRENT_MODIFICATION. `version` NO va en el WHERE: dar de baja es
    // deliberado y debe ejecutarse siempre, no competir con una edicion.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE goods_receipts
            SET enabled = false, version = version + 1
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int softDelete(@Param("id") Long id, @Param("companyId") Long companyId);
}
