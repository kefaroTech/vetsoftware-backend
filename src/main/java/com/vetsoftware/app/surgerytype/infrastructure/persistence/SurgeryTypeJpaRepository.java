package com.vetsoftware.app.surgerytype.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurgeryTypeJpaRepository extends JpaRepository<SurgeryTypeJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<SurgeryTypeJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<SurgeryTypeJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "company")
    @org.springframework.data.jpa.repository.Query("""
            SELECT e
            FROM SurgeryTypeJpaEntity e
            LEFT
            JOIN e.company c
            WHERE e.id = :id
              AND (e.general = true OR c.id = :companyId)
            """)
    Optional<SurgeryTypeJpaEntity> findAvailableById(
            @org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    @EntityGraph(attributePaths = "company")
    List<SurgeryTypeJpaEntity> findAllByGeneralTrueOrCompany_Id(Long companyId);

    /**
     * Lectura ESTRICTA por propiedad, para los caminos de ESCRITURA. A diferencia
     * de {@link #findAvailableById}, que incluye a propósito las filas generales
     * porque sirve a los {@code find}/{@code list}, esta excluye lo que la empresa
     * solo puede consultar: editar, borrar o reactivar una fila general la
     * cambiaría para todos los tenants, y una fila general ajena la reasignaría.
     */
    @EntityGraph(attributePaths = "company")
    Optional<SurgeryTypeJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    /**
     * La fila de la EMPRESA que ocupa ese nombre, esté activa o dada de baja.
     * Nativa para saltar el {@code @SQLRestriction("enabled = true")}: el índice
     * único de la base solo cubre las activas, así que una deshabilitada no ocupa
     * el nombre y el alta tiene que poder verla para reactivarla en vez de chocar.
     * La company se hidrata perezosamente al mapear (el caso de uso corre
     * {@code @Transactional}).
     *
     * <p>
     * La igualdad la resuelve MySQL con la collation de la columna
     * ({@code utf8mb4_0900_ai_ci}), insensible a acentos y a caja — el mismo
     * criterio con el que decide el índice único. Es deliberado: comparar en Java
     * daría «libre» a «Antirrabica» y la base lo rechazaría después.
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
            FROM surgery_types
            WHERE name = :name
              AND company_id = :companyId
            ORDER BY enabled DESC, id DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<SurgeryTypeJpaEntity> findByNameAndCompanyIncludingDisabled(
            @org.springframework.data.repository.query.Param("name") String name,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    /**
     * La gemela para el catálogo de PLATAFORMA: {@code company_id} nulo. Va aparte
     * y no con un parámetro nulable porque {@code = NULL} nunca casa en SQL y el
     * ámbito global se quedaría sin guarda en silencio.
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
            FROM surgery_types
            WHERE name = :name
              AND company_id IS NULL
            ORDER BY enabled DESC, id DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<SurgeryTypeJpaEntity> findGlobalByNameIncludingDisabled(
            @org.springframework.data.repository.query.Param("name") String name);

    // El @SQLRestriction("enabled = true") aplica: cuenta solo filas ACTIVAS, que
    // son las que el indice unico considera. Excluye la fila que se esta editando.
    boolean existsByNameAndCompany_IdAndIdNot(String name, Long companyId, Long id);

    boolean existsByNameAndCompanyIsNullAndIdNot(String name, Long id);

    /**
     * Reactiva y reescribe nombre y descripción en un solo statement, para el alta
     * que se encuentra el nombre ocupado por una fila dada de baja.
     *
     * <p>
     * Nativa por lo mismo que el finder: un {@code merge} de JPA no puede resucitar
     * una fila que el {@code @SQLRestriction} le oculta. Sube {@code version} —la
     * del bloqueo optimista— a propósito: una consulta nativa ni la comprueba ni la
     * incrementa, así que un {@code save} cargado antes reescribiría la fila entera
     * con su {@code enabled = false} y su {@code WHERE version = ?} casaría igual,
     * deshaciendo la reactivación en silencio. Movida la versión, ese save no
     * encuentra fila y salta {@code ObjectOptimisticLockingFailureException} -> 409
     * {@code CONCURRENT_MODIFICATION}.
     */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE surgery_types
            SET enabled = true, name = :name, description = :description,
                version = version + 1
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int reactivateWithDetails(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId,
            @org.springframework.data.repository.query.Param("name") String name,
            @org.springframework.data.repository.query.Param("description") String description);

    /**
     * Sobrecarga del catálogo de PLATAFORMA. El {@code WHERE} nombra igualmente
     * {@code company_id}: acotar por «no tiene empresa» es lo que impide que este
     * camino alcance la fila privada de un tenant.
     */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE surgery_types
            SET enabled = true, name = :name, description = :description,
                version = version + 1
            WHERE id = :id
              AND company_id IS NULL
            """, nativeQuery = true)
    int reactivateWithDetails(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("name") String name,
            @org.springframework.data.repository.query.Param("description") String description);
}
