package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CatalogItemJpaRepository extends JpaRepository<CatalogItemJpaEntity, Long> {

    /**
     * Sube {@code version} porque este {@code UPDATE} va directo a la base: no pasa
     * por el ciclo leer-modificar-guardar, así que {@code @Version} no lo comprueba
     * ni lo incrementa. Sin moverla, un {@code save} concurrente cargado antes
     * reescribe la fila entera desde el dominio —con su {@code enabled = false}— y
     * su {@code WHERE version = ?} casa igual, así que deshace la reactivación en
     * silencio ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}, incidencia #53).
     *
     * <p>
     * Y la versión <strong>no</strong> va en el {@code WHERE}: reactivar es
     * deliberado y debe ejecutarse siempre, no competir con una edición. Ahí solo
     * conseguiría actualizar cero filas y que el servicio lo leyera como «no
     * existe».
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE catalog_items
            SET enabled = true, version = version + 1
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@Param("id") Long id);

    /**
     * Cuenta ignorando el borrado lógico, que es lo que hace también
     * {@code uq_catalog_items_code}: un artículo desactivado sigue ocupando su
     * código. Nativa porque el {@code @SQLRestriction} de la entidad no deja verlo
     * de otra forma.
     *
     * <p>
     * Devuelve {@code long} y no {@code boolean} a propósito: proyectar un literal
     * booleano en una {@code @Query} es exactamente lo que prohíbe la incidencia
     * #196.
     */
    @Query(value = "SELECT COUNT(*) FROM catalog_items WHERE code = :code", nativeQuery = true)
    long countAnyByCode(@Param("code") String code);
}
