package com.vetsoftware.app.spatype.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaTypeJpaRepository extends JpaRepository<SpaTypeJpaEntity, Long> {

    // Sube version porque este UPDATE nativo va directo a la base: no comprueba
    // ni incrementa la version, que @Version solo protege en el ciclo
    // leer-modificar-guardar. Un save concurrente cargado antes reescribe la
    // fila entera desde el dominio, con su enabled = false, y su
    // WHERE version = ? casa igual, deshaciendo la reactivacion en silencio.
    // Movida la version, ese save ya no encuentra fila y salta
    // ObjectOptimisticLockingFailureException -> 409 CONCURRENT_MODIFICATION.
    // La version NO va en el WHERE: reactivar es deliberado y debe ejecutarse
    // siempre, no competir con una edicion.
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE spa_types
            SET enabled = true, version = version + 1
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    /**
     * La fila que ocupa ese nombre, esté activa o dada de baja. Nativa para saltar
     * el {@code @SQLRestriction("enabled = true")}: el índice único cubre solo las
     * activas, así que una deshabilitada no ocupa el nombre y el alta tiene que
     * poder verla para reactivarla en vez de chocar.
     *
     * <p>
     * La igualdad la resuelve MySQL con la collation de la columna
     * ({@code utf8mb4_0900_ai_ci}), insensible a acentos y a caja — el mismo
     * criterio del índice único. Comparar en Java daría «libre» a «Bano
     * terapeutico» frente a «Baño terapéutico».
     *
     * <p>
     * El {@code ORDER BY enabled DESC, id DESC} + {@code LIMIT 1} NO es cosmetico y
     * el {@code Optional} depende de el. El indice unico cubre solo las filas
     * ACTIVAS —{@code active_name} vale NULL cuando {@code enabled = false} y MySQL
     * no deduplica NULL—, asi que la tabla admite UNA activa y N dadas de baja con
     * el mismo nombre. Sin orden ni limite, la segunda baja homonima convertia esta
     * consulta en un {@code IncorrectResultSizeDataAccessException} —un 500— y
     * dejaba ese nombre inutilizable para siempre (#580).
     *
     * <p>
     * El criterio del orden es el que necesita la guarda: la fila ACTIVA primero,
     * porque es la unica que de verdad ocupa el nombre y la que debe hacer saltar
     * el conflicto; si no hay ninguna activa, la de {@code id} mayor, es decir la
     * que se creo mas tarde. Ojo con la promesa: {@code id DESC} ordena por
     * CREACION, no por fecha de baja, porque estas tablas no guardan cuando se dio
     * de baja una fila. Es el mejor proxy disponible y basta para que la consulta
     * sea determinista, que es lo que aqui hace falta.
     */
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT *
            FROM spa_types
            WHERE name = :name
            ORDER BY enabled DESC, id DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<SpaTypeJpaEntity> findByNameIncludingDisabled(
            @org.springframework.data.repository.query.Param("name") String name);

    // El @SQLRestriction("enabled = true") aplica: cuenta solo filas ACTIVAS, que
    // son las que el indice unico considera.
    boolean existsByNameAndIdNot(String name, Long id);

    /**
     * Reactiva y reescribe nombre y descripción en un solo statement, para el alta
     * que se encuentra el nombre ocupado por una fila dada de baja.
     *
     * <p>
     * Sube {@code version} por la misma razón que {@link #reactivate(Long)}: una
     * consulta nativa ni comprueba ni incrementa el bloqueo optimista, y sin el
     * bump un {@code save} cargado antes deshace la reactivación en silencio.
     */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE spa_types
            SET enabled = true, name = :name, description = :description,
                version = version + 1
            WHERE id = :id
            """, nativeQuery = true)
    int reactivateWithDetails(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("name") String name,
            @org.springframework.data.repository.query.Param("description") String description);
}
