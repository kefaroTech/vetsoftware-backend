package com.vetsoftware.app.medicament.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Catalogo COMPLETO de la plataforma —globales mas los privados de cada
     * empresa—, paginado y con busqueda opcional por nombre. Es la vista de
     * contexto de la consola, cerrada a {@code ROLE_SYSTEM}.
     *
     * <p>
     * {@code :q} nulo significa «sin filtro» y devuelve exactamente lo que devolvia
     * antes de existir la busqueda; el adaptador solo recorta el termino y traduce
     * el blanco a nulo. El {@code LOWER(...) LIKE LOWER(CONCAT('%', :q, '%'))} es
     * literalmente el de {@code CompanyJpaRepository.searchByTerm} y
     * {@code OwnerJpaRepository.searchByCompanyAndTerm}, que es como el repositorio
     * resuelve la busqueda paginada en JPQL; el parametro se llama {@code q} porque
     * es el que usan los catorce controladores que ya buscan.
     *
     * <p>
     * JPQL y no SQL nativo —al reves que la de empleados— por dos cosas que aqui si
     * importan: el {@code @SQLRestriction("enabled = true")} de la entidad se sigue
     * aplicando (una nativa lo salta y habria que reescribir el filtro a mano), y
     * el {@code @EntityGraph} tambien, que en esta consulta NO es decorativo: las
     * filas privadas si tienen empresa y el mapper lee su nombre, asi que sin el
     * son N+1 consultas por pagina. Spring Data deriva sola la de conteo.
     *
     * <p>
     * <b>Buscar y chocar responden al mismo criterio, y eso es correccion y no
     * gusto.</b> El indice unico decide un choque de nombre con la collation de la
     * columna, y esta comparacion tambien: el {@code LIKE} sobre {@code e.name}
     * toma la collation de la columna, no la del literal. Por eso NO se normaliza
     * nada en Java —ni caja ni acentos—: seria el unico modo de que la busqueda
     * fuera mas ESTRICTA que la guarda, y entonces el operador buscaria
     * «Cloxacilina», no la encontraria, la crearia y recibiria un 409 sobre
     * exactamente lo que acaba de buscar sin exito. El {@code LOWER} solo puede
     * hacer la busqueda mas permisiva que la guarda, nunca al reves, que es la
     * direccion inofensiva.
     *
     * <p>
     * La collation efectiva NO esta declarada en ningun {@code CREATE TABLE} del
     * repositorio: se hereda del servidor. Los indicios apuntan a
     * {@code utf8mb4_0900_ai_ci} —insensible a caja y acentos— y el mas solido no
     * es la documentacion sino el changeset {@code 292}, que tuvo que escribir
     * {@code WHERE c.name COLLATE utf8mb4_bin = 'Cafe'} para distinguir la fila sin
     * tilde de la que ya tenia «Café»: forzar binario solo hace falta cuando la
     * collation ambiente las confunde. Pero el razonamiento de arriba no depende de
     * eso: sea cual sea la collation, es la MISMA para el indice unico y para esta
     * consulta.
     */
    @EntityGraph(attributePaths = "company")
    @Query("""
            SELECT e
            FROM MedicamentJpaEntity e
            WHERE (:q IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<MedicamentJpaEntity> search(@Param("q") String q, Pageable pageable);

    /**
     * El catalogo GLOBAL activo, paginado y con busqueda opcional por nombre: la
     * vista que administra la consola de plataforma. El
     * {@code @SQLRestriction("enabled = true")} aplica, asi que los pausados salen
     * por {@link #findAllDisabledGlobal()} y no por aqui.
     *
     * <p>
     * Mismo contrato de busqueda que {@link #search(String, Pageable)}: {@code :q}
     * nulo es «sin filtro», la subcadena la arma el adaptador y la insensibilidad a
     * caja y acentos la pone la collation. Con 153 moleculas sembradas y paginas de
     * 20, sin esto encontrar una es pasar seis paginas.
     *
     * <p>
     * El {@code @EntityGraph} es formalmente inutil aqui —un general no tiene
     * empresa que hidratar— y se declara igual: es la invariante que la regla de
     * N+1 comprueba, y seguiria siendo correcta el dia que esta consulta deje de
     * estar acotada a los generales.
     */
    @EntityGraph(attributePaths = "company")
    @Query("""
            SELECT e
            FROM MedicamentJpaEntity e
            WHERE e.general = true
              AND (:q IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<MedicamentJpaEntity> searchGlobal(@Param("q") String q, Pageable pageable);

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
     * La gemela del vademecum de PLATAFORMA, y la razon por la que va aparte en vez
     * de aceptar un {@code companyId} nulable: {@code company_id = NULL} no casa
     * NUNCA en SQL, ni siquiera con las filas que tienen la columna nula. Con la
     * consulta acotada, esta lista salia siempre vacia y un global pausado se
     * quedaba sin ninguna pantalla desde la que reactivarlo — invisible en el
     * catalogo activo por el {@code @SQLRestriction} y ausente de aqui. Mismo
     * motivo, mismo remedio y misma forma que
     * {@link #findGlobalByNameIncludingDisabled(String)}.
     *
     * <p>
     * El {@code ORDER BY} tampoco es cosmetico (#594). Esta consulta no pagina, y
     * sin orden explicito MySQL devuelve las filas en el orden que le convenga —el
     * del indice que acabe usando—, asi que la pantalla de pausados barajaba su
     * contenido entre recargas mientras su hermana {@link #searchGlobal} si
     * ordenaba. El {@code id} desempata porque aqui SI puede haber homonimos: el
     * indice unico cubre solo las filas activas ({@code active_name} vale NULL
     * cuando {@code enabled = false}), asi que la tabla admite N pausadas con el
     * mismo nombre y sin el desempate el orden seguiria siendo arbitrario entre
     * ellas.
     */
    @Query(value = """
            SELECT *
            FROM medicaments
            WHERE enabled = false
              AND company_id IS NULL
            ORDER BY name ASC, id ASC
            """, nativeQuery = true)
    List<MedicamentJpaEntity> findAllDisabledGlobal();

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
     * La gemela para el vademecum de PLATAFORMA. Existe por lo mismo que
     * {@link #findGlobalByNameIncludingDisabled(String)} y
     * {@link #findAllDisabledGlobal()}: {@code company_id = :companyId} con
     * {@code companyId} nulo no casa ninguna fila, asi que reactivar un global por
     * la consulta acotada afectaba cero filas y el servicio lo traducia a un 404
     * sobre una fila que existe. El resultado practico era un global pausado
     * IRRECUPERABLE.
     *
     * <p>
     * El {@code company_id IS NULL} no es cosmetico: es lo que impide que este
     * camino —que no recibe empresa alguna— alcance el medicamento privado de un
     * tenant. Aqui no hay lectura previa que valide la propiedad, igual que en
     * {@link #reactivate(Long, Long)}: el {@code WHERE} es toda la barrera.
     *
     * <p>
     * Sube {@code version} por la misma razon que sus hermanas: una consulta nativa
     * ni comprueba ni incrementa el bloqueo optimista, y sin el bump un
     * {@code save} cargado antes reescribe la fila entera con su
     * {@code enabled = false} y deshace la reactivacion en silencio.
     *
     * <p>
     * <b>El {@code AND enabled = false} no sobra</b> (#484). Sin el, reactivar una
     * fila que YA estaba activa cuenta como exito: afecta una fila, sube la
     * {@code version} y el endpoint responde 200 sin haber recuperado nada. El dano
     * no es el 200 sino el bump: quien tuviera la ficha abierta guarda su edicion
     * con la version vieja, el {@code WHERE version = ?} de Hibernate no casa y
     * recibe un 409 {@code CONCURRENT_MODIFICATION} que no corresponde a ninguna
     * edicion concurrente real. Se alinea con {@code EmployeeJpaRepository}, que es
     * quien ya hacia esta distincion: reactivar es deliberado y debe ejecutarse
     * siempre, pero reactivar algo que ya esta activo no es una reactivacion.
     *
     * <p>
     * A cambio, el servicio traduce las cero filas a un 404 tambien cuando el
     * global existe y ya estaba activo. Es el precio de la semantica de
     * {@code employees} y en esta pantalla apenas se paga: la consola solo ofrece
     * reactivar sobre lo que le devuelve {@link #findAllDisabledGlobal()}, que por
     * definicion esta pausado, asi que el 404 solo aparece en una carrera de dos
     * clics — donde es mejor respuesta que el 409 fantasma de despues. Que el
     * mensaje ideal seria «ya estaba activo» y no «no existe» es cierto, y es justo
     * la decision transversal que #484 tiene abierta para los 34 {@code reactivate}
     * del repositorio: no se resuelve en una sola rodaja.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE medicaments
            SET enabled = true, version = version + 1
            WHERE id = :id
              AND company_id IS NULL
              AND enabled = false
            """, nativeQuery = true)
    int reactivateGlobal(@Param("id") Long id);

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
