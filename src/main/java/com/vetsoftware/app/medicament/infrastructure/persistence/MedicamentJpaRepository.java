package com.vetsoftware.app.medicament.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MedicamentJpaRepository extends JpaRepository<MedicamentJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<MedicamentJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<MedicamentJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "company")
    @org.springframework.data.jpa.repository.Query("""
            SELECT e
            FROM MedicamentJpaEntity e
            LEFT
            JOIN e.company c
            WHERE e.id = :id
              AND (e.general = true OR c.id = :companyId)
            """)
    Optional<MedicamentJpaEntity> findAvailableById(
            @org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    /**
     * Estrictamente el medicamento PROPIO de la empresa. Distinto de
     * {@link #findAvailableById}, que ademas devuelve los generales: para leer y
     * recetar sirve el catalogo disponible, pero escribir —editar, borrar,
     * reactivar— solo puede alcanzar lo que la empresa creo. Un general
     * ({@code company_id} NULL) es de la plataforma y no lo toca ningun tenant.
     */
    @EntityGraph(attributePaths = "company")
    Optional<MedicamentJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @EntityGraph(attributePaths = "company")
    List<MedicamentJpaEntity> findAllByGeneralTrueOrCompany_Id(Long companyId);

    // Native: los pausados (enabled = false) NO pasan el @SQLRestriction; se listan
    // crudos para
    // reactivar.
    @Query(value = """
            SELECT *
            FROM medicaments
            WHERE enabled = false
              AND company_id = :companyId
            """, nativeQuery = true)
    List<MedicamentJpaEntity> findAllDisabledForCompany(@Param("companyId") Long companyId);

    /**
     * El filtro por {@code company_id} no es defensa en profundidad: es LA defensa.
     * En la reactivacion no hay lectura previa que valide la propiedad —el servicio
     * decide si existe mirando las filas afectadas—, asi que un UPDATE por id a
     * secas resucitaba el medicamento pausado de cualquier tenant para quien
     * conociera el id.
     *
     * <p>
     * El UPDATE mueve tambien {@code version}, la del bloqueo optimista, a
     * proposito: una consulta nativa no la comprueba ni la incrementa, asi que un
     * save cargado antes de la reactivacion reescribia la fila entera desde el
     * dominio —el mapper la copia— y su {@code WHERE version = ?} casaba igual,
     * deshaciendo en silencio el {@code enabled = true}. Movida la version, ese
     * save ya no encuentra fila y salta
     * {@code ObjectOptimisticLockingFailureException} -> 409
     * {@code CONCURRENT_MODIFICATION}. {@code version} NO va en el {@code WHERE}:
     * reactivar es deliberado y debe ejecutarse siempre, no competir con una
     * edicion.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE medicaments
            SET enabled = true, version = version + 1
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int reactivate(@Param("id") Long id, @Param("companyId") Long companyId);

    /**
     * El medicamento de la EMPRESA que ocupa ese nombre, activo o pausado. Nativa
     * para saltar el {@code @SQLRestriction("enabled = true")}: el indice unico de
     * la base solo cubre los activos, asi que uno pausado no ocupa el nombre y el
     * alta tiene que poder verlo para reactivarlo en vez de chocar. La company se
     * hidrata perezosamente al mapear (el caso de uso corre
     * {@code @Transactional}).
     *
     * <p>
     * La igualdad la resuelve MySQL con la collation de la columna
     * ({@code utf8mb4_0900_ai_ci}), insensible a acentos y a caja: el mismo
     * criterio con el que decide el indice unico.
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
    @Query(value = """
            SELECT *
            FROM medicaments
            WHERE name = :name
              AND company_id = :companyId
            ORDER BY enabled DESC, id DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<MedicamentJpaEntity> findByNameAndCompanyIncludingDisabled(@Param("name") String name,
            @Param("companyId") Long companyId);

    /**
     * La gemela para el vademecum de PLATAFORMA: {@code company_id} nulo. Va aparte
     * y no con un parametro nulable porque {@code = NULL} nunca casa en SQL y el
     * ambito global se quedaria sin guarda en silencio.
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
    @Query(value = """
            SELECT *
            FROM medicaments
            WHERE name = :name
              AND company_id IS NULL
            ORDER BY enabled DESC, id DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<MedicamentJpaEntity> findGlobalByNameIncludingDisabled(@Param("name") String name);

    // El @SQLRestriction("enabled = true") aplica: cuenta solo filas ACTIVAS, que
    // son las que el indice unico considera. Excluye la fila que se esta editando.
    boolean existsByNameAndCompany_IdAndIdNot(String name, Long companyId, Long id);

    boolean existsByNameAndCompanyIsNullAndIdNot(String name, Long id);

    /**
     * Reactiva y reescribe nombre y descripcion en un solo statement, para el alta
     * que se encuentra el nombre ocupado por una fila pausada.
     *
     * <p>
     * Sube {@code version} por la misma razon que {@link #reactivate(Long, Long)}:
     * una consulta nativa ni comprueba ni incrementa el bloqueo optimista, y sin el
     * bump un {@code save} cargado antes reescribe la fila entera con su
     * {@code enabled = false} y deshace la reactivacion en silencio.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE medicaments
            SET enabled = true, name = :name, description = :description,
                version = version + 1
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int reactivateWithDetails(@Param("id") Long id, @Param("companyId") Long companyId,
            @Param("name") String name, @Param("description") String description);

    /**
     * Sobrecarga del vademecum de PLATAFORMA. El {@code WHERE} nombra igualmente
     * {@code company_id}: acotar por «no tiene empresa» es lo que impide que este
     * camino alcance el medicamento privado de un tenant.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE medicaments
            SET enabled = true, name = :name, description = :description,
                version = version + 1
            WHERE id = :id
              AND company_id IS NULL
            """, nativeQuery = true)
    int reactivateWithDetails(@Param("id") Long id, @Param("name") String name,
            @Param("description") String description);
}
