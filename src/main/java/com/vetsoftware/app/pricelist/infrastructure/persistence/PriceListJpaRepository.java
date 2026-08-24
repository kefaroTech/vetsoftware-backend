package com.vetsoftware.app.pricelist.infrastructure.persistence;

import com.vetsoftware.app.pricelist.domain.PriceListStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PriceListJpaRepository extends JpaRepository<PriceListJpaEntity, Long> {

    /**
     * Derivada del nombre y no nativa: aqui interesa justamente lo contrario que en
     * las tres consultas de mas abajo. Estas ven solo las listas activas -es el
     * {@code @SQLRestriction} de la entidad haciendo su trabajo-, que es lo
     * correcto para un desplegable; aquellas existen precisamente para esquivarlo.
     */
    Page<PriceListJpaEntity> findAllByStatus(PriceListStatus status, Pageable pageable);

    /**
     * Carga la lista tomando un {@code PESSIMISTIC_WRITE} sobre su fila. Es lo que
     * serializa el read-then-write de la comprobacion de solape de tramos de sus
     * precios: sin el, dos altas concurrentes leen el mismo conjunto de hermanos y
     * las dos pasan la comprobacion.
     *
     * <p>
     * No lleva {@code version} en el WHERE ni la mueve: es un SELECT, y el bloqueo
     * optimista de la propia lista lo sigue haciendo {@code @Version} en el flujo
     * normal de guardado.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM PriceListJpaEntity p
            WHERE p.id = :id
            """)
    Optional<PriceListJpaEntity> lockById(@Param("id") Long id);

    /**
     * Deshace la baja logica. El {@code SET} mueve tambien {@code version}
     * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}, #53): sin eso, un {@code save}
     * cargado antes de la reactivacion reescribe {@code enabled} con su valor viejo
     * -el mapper lo copia desde el dominio- y su {@code WHERE version = ?} casa
     * igual, con lo que una edicion concurrente vuelve a apagar en silencio lo que
     * la reactivacion acababa de encender. Movida la version, ese {@code save} no
     * encuentra fila y salta {@code ObjectOptimisticLockingFailureException} -> 409
     * CONCURRENT_MODIFICATION.
     *
     * <p>
     * {@code version} NO va en el {@code WHERE}: reactivar es una operacion
     * deliberada y debe ejecutarse siempre, no competir con una edicion.
     */
    /**
     * El id de la lista con ese codigo <strong>ignorando el borrado
     * logico</strong>, que es como lo mira {@code uq_price_lists_code}: una lista
     * retirada sigue ocupando su codigo aunque la aplicacion no la vea. Nativa
     * porque es la unica forma de esquivar el {@code @SQLRestriction} de la
     * entidad.
     */
    @Query(value = "SELECT id FROM price_lists WHERE code = :code", nativeQuery = true)
    Optional<Long> findAnyIdByCode(@Param("code") String code);

    /**
     * Devuelve {@code long} y no {@code boolean} a proposito: proyectar un literal
     * booleano en una {@code @Query} es exactamente lo que prohibe
     * {@code PROYECCION_SIN_LITERAL_BOOLEANO} (incidencia #196).
     */
    @Query(value = "SELECT COUNT(*) FROM price_lists WHERE code = :code AND enabled = TRUE", nativeQuery = true)
    long countEnabledByCode(@Param("code") String code);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE price_lists
            SET enabled = true, version = version + 1
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@Param("id") Long id);
}
