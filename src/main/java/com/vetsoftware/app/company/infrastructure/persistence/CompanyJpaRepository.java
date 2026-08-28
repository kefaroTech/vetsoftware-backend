package com.vetsoftware.app.company.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyJpaRepository extends JpaRepository<CompanyJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "city")
    List<CompanyJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "city")
    Optional<CompanyJpaEntity> findById(Long id);

    // La asociación del grafo es to-one, así que el JOIN FETCH convive con
    // la paginación sin traerse la tabla a memoria. Con una colección habría que
    // separar la consulta o Hibernate paginaría en el heap (HHH000104).
    @Override
    @EntityGraph(attributePaths = "city")
    Page<CompanyJpaEntity> findAll(Pageable pageable);

    /**
     * La empresa propia servida como página de una fila. Existe para que el
     * adaptador tenga una sola forma de responder —{@code Page} siempre— y no tenga
     * que fabricar a mano los metadatos de la página del empleado: con
     * {@code page=1} la consulta devuelve contenido vacío y {@code totalElements=1}
     * sola, sin aritmética que equivocar.
     */
    @EntityGraph(attributePaths = "city")
    @Query("""
            SELECT c
            FROM CompanyJpaEntity c
            WHERE c.id = :companyId
            """)
    Page<CompanyJpaEntity> findPageByCompanyId(@Param("companyId") Long companyId,
            Pageable pageable);

    @EntityGraph(attributePaths = "city")
    @Query("""
            SELECT c
            FROM CompanyJpaEntity c
            WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(c.identifier) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<CompanyJpaEntity> searchByTerm(@Param("query") String query, Pageable pageable);

    @EntityGraph(attributePaths = "city")
    @Query("""
            SELECT c
            FROM CompanyJpaEntity c
            WHERE c.id = :companyId
              AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(c.identifier) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<CompanyJpaEntity> searchByCompanyAndTerm(@Param("companyId") Long companyId,
            @Param("query") String query, Pageable pageable);

    /**
     * El ARCHIVO completo, paginado. <b>Nativa y no JPQL, por la unica razon por la
     * que existe este metodo</b>: un JPQL sobre {@code CompanyJpaEntity} arrastra
     * el {@code @SQLRestriction("enabled = true")} al {@code WHERE} y devuelve
     * siempre cero filas. Mismo mecanismo que {@link #reactivate(Long)} y que los
     * cinco {@code /disabled} ya existentes del sistema
     * ({@code MedicamentJpaRepository.findAllDisabledForCompany} y sus hermanos).
     *
     * <p>
     * <b>El {@code ORDER BY} va embebido en el SQL y el {@code Pageable} llega sin
     * {@code Sort}</b>: en una consulta nativa Spring Data no traduce los nombres
     * de propiedad del {@code Sort}, los concatena crudos como nombres de COLUMNA,
     * de modo que un {@code Sort.by("createdDate")} generaria SQL invalido en
     * tiempo de ejecucion. Precedente:
     * {@code EmployeeJpaRepository.searchByCompanyIncludingDisabled}.
     *
     * <p>
     * El desempate por {@code id} no sobra: sin un orden total, dos empresas
     * homonimas pueden cambiar de pagina entre dos peticiones y una fila repetirse
     * mientras otra no aparece nunca.
     *
     * <p>
     * <b>Sin {@code @EntityGraph}, que una consulta nativa no admite</b>:
     * {@code city} se hidrata perezosamente al mapear, y por eso
     * {@code ListDisabledCompaniesService} corre
     * {@code @Transactional(readOnly = true)} —con {@code open-in-view: false} no
     * hay sesion en la capa web—.
     */
    @Query(value = """
            SELECT *
            FROM companies
            WHERE enabled = false
            ORDER BY name ASC, id ASC
            """, countQuery = """
            SELECT COUNT(*)
            FROM companies
            WHERE enabled = false
            """, nativeQuery = true)
    Page<CompanyJpaEntity> findDisabledPage(Pageable pageable);

    /**
     * La misma pagina acotada a UNA empresa. En {@code companies} el tenant es la
     * propia fila, asi que el filtro es {@code id = :companyId} — el mismo criterio
     * que {@link #findPageByCompanyId}, que es su gemela para las activas.
     *
     * <p>
     * <b>Este {@code AND} es la barrera, no un refuerzo.</b> Al saltarse el
     * {@code @SQLRestriction} la consulta se salta tambien cualquier suposicion de
     * que «algo mas» esta mirando: no hay {@code findById} previo del que colgar
     * una comprobacion de propiedad, porque una fila archivada no la devuelve
     * ningun {@code findById}. Si este predicado desaparece, un empleado con
     * {@code company.read} lee el archivo mercantil de todos los tenants y no falla
     * nada: responde 200. Es la leccion de {@code releaseFromVoidedDocument},
     * escrita en una consulta de lectura.
     */
    @Query(value = """
            SELECT *
            FROM companies
            WHERE enabled = false
              AND id = :companyId
            ORDER BY name ASC, id ASC
            """, countQuery = """
            SELECT COUNT(*)
            FROM companies
            WHERE enabled = false
              AND id = :companyId
            """, nativeQuery = true)
    Page<CompanyJpaEntity> findDisabledPageByCompanyId(@Param("companyId") Long companyId,
            Pageable pageable);

    boolean existsByIdentifier(String identifier);

    /**
     * Saca a la empresa del archivo. El inverso del {@code @SQLDelete} de la
     * entidad.
     *
     * <p>
     * <b>Nativa y no JPQL, por la misma razon que existe este metodo</b>: un
     * {@code UPDATE} en JPQL sobre {@code CompanyJpaEntity} arrastraria el
     * {@code @SQLRestriction("enabled = true")} al {@code WHERE} y no encontraria
     * jamas la fila que intenta reactivar. El SQL nativo no pasa por la
     * restriccion.
     *
     * <p>
     * <b>{@code version = version + 1} no es opcional</b> ({@code #53,
     * UPDATE_MASIVO_MUEVE_LA_VERSION}): {@code companies} va versionada, y un
     * {@code UPDATE} masivo no pasa por el ciclo leer-modificar-guardar de
     * Hibernate —ni comprueba ni incrementa nada—. Sin mover la version, un
     * {@code save} concurrente que viniera de una lectura anterior casaria con la
     * version vieja y desharia la reactivacion <b>sin excepcion y sin log</b>. La
     * version va en el {@code SET} y nunca en el {@code WHERE}: condicionarla
     * dejaria la restauracion en cero filas y el servicio lo leeria como «esta
     * empresa no existe».
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE companies
            SET enabled = true,
                version = version + 1
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@Param("id") Long id);

}
