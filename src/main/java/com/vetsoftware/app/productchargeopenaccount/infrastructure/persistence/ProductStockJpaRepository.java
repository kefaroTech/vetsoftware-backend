package com.vetsoftware.app.productchargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Segundo repositorio sobre {@code products} (Spring Data permite varios por entidad), dedicado al ajuste
 * atómico de stock desde la venta. Se usa UPDATE nativo — no el save() del catálogo — para:
 *   1) ser atómico e inmune a lost-updates entre ventas concurrentes (el motor serializa el UPDATE de la fila);
 *   2) permitir stock negativo sin guard (decisión de negocio);
 *   3) bumpear `version` para que una edición concurrente del producto (optimistic lock) detecte el cambio.
 */
public interface ProductStockJpaRepository extends JpaRepository<ProductJpaEntity, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE products SET current_stock = current_stock - :qty, version = version + 1 "
        + "WHERE id = :id AND company_id = :companyId", nativeQuery = true)
    int decreaseStock(@Param("id") Long id, @Param("companyId") Long companyId, @Param("qty") int qty);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE products SET current_stock = current_stock + :qty, version = version + 1 "
        + "WHERE id = :id AND company_id = :companyId", nativeQuery = true)
    int increaseStock(@Param("id") Long id, @Param("companyId") Long companyId, @Param("qty") int qty);
}
