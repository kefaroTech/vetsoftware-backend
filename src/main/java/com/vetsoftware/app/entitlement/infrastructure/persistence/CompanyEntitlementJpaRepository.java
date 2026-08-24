package com.vetsoftware.app.entitlement.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas de {@code company_entitlements}. <strong>Es lo que el resto del
 * sistema mira en cada peticion</strong>, asi que ninguna consulta de aqui
 * puede barrer la tabla: todas entran por {@code uq_company_entitlements
 * (company_id, sub_module_id)}.
 *
 * <p>
 * Las tres consultas de permisos comparten el mismo filtro de vigencia
 * --ventana abierta <em>ahora</em>-- porque esa es justamente la diferencia con
 * el modelo que sustituyen: {@code membership_sub_modules} decia lo que el plan
 * incluye, y estas dicen lo que la empresa puede usar hoy. Sin la ventana, una
 * prueba caducada seguiria dando acceso.
 */
public interface CompanyEntitlementJpaRepository
        extends
            JpaRepository<CompanyEntitlementJpaEntity, Long> {

    @EntityGraph(attributePaths = "subModule")
    List<CompanyEntitlementJpaEntity> findAllByCompany_Id(Long companyId);

    @EntityGraph(attributePaths = "subModule")
    Page<CompanyEntitlementJpaEntity> findAllByCompany_Id(Long companyId, Pageable pageable);

    @EntityGraph(attributePaths = "subModule")
    Optional<CompanyEntitlementJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    /**
     * Las filas de la empresa con ese origen. La usa el recalculo para leer las
     * {@code MANUAL_GRANT} antes de borrar: son las unicas que no puede
     * reconstruir, porque no salen de ningun contrato.
     */
    @EntityGraph(attributePaths = "subModule")
    List<CompanyEntitlementJpaEntity> findAllByCompany_IdAndSource(Long companyId, String source);

    /**
     * Que submodulos puede usar esta empresa. La respuesta a la pregunta caliente,
     * en un rango sobre el prefijo {@code company_id} del indice unico.
     *
     * <p>
     * <strong>El {@code JOIN} contra {@code sub_modules} no es decorativo</strong>
     * (#414). Esta consulta alimenta el alta de empresa, que <em>escribe</em> filas
     * de {@code permissions} y {@code role_permissions}. Sin el filtro por
     * {@code enabled} el alta seguia creandolas sobre un submodulo apagado: no
     * habia fuga --{@link #findEffectivePermissionCodes} las descarta en cada
     * peticion-- pero el estado se acumulaba, y el dia que alguien reactivara ese
     * submodulo para una prueba puntual, todas las empresas que lo acumularon
     * recuperaban sus authorities de golpe sin que nadie hubiera decidido cuales
     * debian tenerlas. Las cuatro consultas de concesion de este repositorio
     * responden ahora lo mismo.
     */
    @Query(value = """
            SELECT e.sub_module_id
            FROM company_entitlements e
            JOIN sub_modules s ON s.id = e.sub_module_id AND s.enabled = TRUE
            WHERE e.company_id = :companyId
              AND e.access_level IN (:accessLevels)
              AND e.valid_from <= CURRENT_TIMESTAMP(6)
              AND (e.valid_until IS NULL OR e.valid_until > CURRENT_TIMESTAMP(6))
            """, nativeQuery = true)
    List<Long> findGrantedSubModuleIdsByCompanyId(@Param("companyId") Long companyId,
            @Param("accessLevels") Collection<String> accessLevels);

    /**
     * Permisos efectivos de un empleado: la asignacion base debe pertenecer a la
     * misma empresa y tener un entitlement vigente. READ_ONLY conserva solo la
     * autoridad de lectura; FULL conserva cualquier autoridad base.
     */
    @Query(value = """
            SELECT DISTINCT p.code
            FROM permissions p
            JOIN sub_modules sm
              ON sm.id = p.sub_module_id
             AND sm.enabled = TRUE
            JOIN company_entitlements e
              ON e.company_id = p.company_id
             AND e.sub_module_id = p.sub_module_id
            WHERE p.company_id = :companyId
              AND p.enabled = TRUE
              AND p.code IN (:permissionCodes)
              AND e.valid_from <= CURRENT_TIMESTAMP(6)
              AND (e.valid_until IS NULL OR e.valid_until > CURRENT_TIMESTAMP(6))
              AND (e.access_level = 'FULL'
                   OR (e.access_level = 'READ_ONLY' AND p.code LIKE '%.read'))
            """, nativeQuery = true)
    Set<String> findEffectivePermissionCodes(@Param("companyId") Long companyId,
            @Param("permissionCodes") Collection<String> permissionCodes);

    /**
     * La misma pregunta para varias empresas de un golpe, en pares. Existe para que
     * quien resuelve un lote no haga una consulta por empresa: ese N+1 es invisible
     * en desarrollo con dos clinicas y se nota con cien.
     *
     * <p>
     * Lleva el mismo {@code JOIN} por {@code enabled} que su hermana de una sola
     * empresa, y por el mismo motivo (#414): la republicacion masiva tambien
     * <em>escribe</em> permisos, y una asimetria entre las dos habria dejado el
     * agujero abierto justo en el camino que toca a todos los tenants a la vez.
     */
    @Query(value = """
            SELECT e.company_id AS companyId, e.sub_module_id AS subModuleId
            FROM company_entitlements e
            JOIN sub_modules s ON s.id = e.sub_module_id AND s.enabled = TRUE
            WHERE e.company_id IN (:companyIds)
              AND e.access_level IN (:accessLevels)
              AND e.valid_from <= CURRENT_TIMESTAMP(6)
              AND (e.valid_until IS NULL OR e.valid_until > CURRENT_TIMESTAMP(6))
            """, nativeQuery = true)
    List<CompanySubModuleGrantView> findGrantedSubModuleIdsByCompanyIdIn(
            @Param("companyIds") Collection<Long> companyIds,
            @Param("accessLevels") Collection<String> accessLevels);

    /**
     * Cuenta los permisos vigentes de esta empresa sobre el submodulo con ese
     * codigo: la base de "esta empresa tiene BILLING?".
     *
     * <p>
     * <strong>Devuelve {@code long} y no {@code boolean} a proposito, y no se puede
     * cambiar.</strong> Sustituye a
     * {@code MembershipSubModuleJpaRepository.hasEnabledSubModuleCode}, cuya
     * version original proyectaba un literal booleano
     * ({@code SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END}) y con
     * Hibernate 7 fallaba el 100 % de las veces con
     * {@code Cannot coerce value 'true' [java.lang.Boolean] to Integer}. Aquello
     * dejo caida la facturacion electronica entera sin que ninguna prueba lo viera.
     * De ahi salio la regla {@code PROYECCION_SIN_LITERAL_BOOLEANO} (#196): la
     * consulta cuenta y el adaptador compara contra cero.
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM company_entitlements e
            JOIN sub_modules s ON s.id = e.sub_module_id AND s.enabled = TRUE
            WHERE e.company_id = :companyId
              AND s.code = :subModuleCode
              AND e.access_level IN (:accessLevels)
              AND e.valid_from <= CURRENT_TIMESTAMP(6)
              AND (e.valid_until IS NULL OR e.valid_until > CURRENT_TIMESTAMP(6))
            """, nativeQuery = true)
    long countGrantedByCompanyIdAndSubModuleCode(@Param("companyId") Long companyId,
            @Param("subModuleCode") String subModuleCode,
            @Param("accessLevels") Collection<String> accessLevels);

    /**
     * Cuantas filas de entitlement tiene esta empresa, <strong>sin filtrar por
     * vigencia, por nivel ni por submodulo vivo</strong>. No responde "¿que puede
     * usar?" sino "¿se le ha calculado esto alguna vez?".
     *
     * <p>
     * Es el desempate de #410. {@link #findEffectivePermissionCodes} devuelve el
     * conjunto vacio en dos situaciones que no se parecen en nada: la empresa tiene
     * sus entitlements calculados y este empleado no alcanza ninguno --normal, un
     * recepcionista de una clinica que solo contrato agenda--, o la empresa <em>no
     * tiene ni una fila</em>, y entonces TODOS sus empleados ven 403 en todo y
     * nadie puede distinguirlo de "este usuario no tiene permisos". Cero aqui
     * significa lo segundo y hay que gritarlo.
     *
     * <p>
     * Deliberadamente sin ventana de vigencia: una empresa cuyas filas hayan
     * caducado <em>si</em> fue calculada, y ese es un problema de contrato, no de
     * proceso caido. Mezclar los dos casos volveria a colapsar lo que esta consulta
     * existe para separar.
     *
     * <p>
     * Devuelve {@code long} y la comparacion con cero se hace en Java:
     * {@code PROYECCION_SIN_LITERAL_BOOLEANO} (#196).
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM company_entitlements e
            WHERE e.company_id = :companyId
            """, nativeQuery = true)
    long countByCompanyId(@Param("companyId") Long companyId);

    /**
     * ¿Alguna empresa tiene concedido <strong>hoy</strong> este submodulo? La
     * guarda del lado del inquilino que faltaba al apagar un submodulo (#413).
     *
     * <p>
     * <strong>Es la unica consulta del repositorio que no entra por
     * {@code company_id}</strong>, y no barre la tabla igualmente: entra por el
     * indice que InnoDB mantiene para la clave foranea
     * {@code fk_company_entitlements_sub_module}, que existe porque
     * {@code uq_company_entitlements (company_id, sub_module_id)} no sirve de
     * indice de esa FK --{@code sub_module_id} no es su prefijo--. La pregunta es
     * legitimamente global: apagar un submodulo es una operacion de plataforma que
     * afecta a todos los tenants a la vez, asi que acotarla a una empresa seria
     * responder otra pregunta.
     *
     * <p>
     * {@code NONE} queda fuera: es explicitamente "este submodulo no existe para
     * esta empresa", asi que no hay acceso vigente que proteger.
     *
     * <p>
     * Devuelve {@code long} por {@code PROYECCION_SIN_LITERAL_BOOLEANO} (#196).
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM company_entitlements e
            WHERE e.sub_module_id = :subModuleId
              AND e.access_level <> 'NONE'
              AND e.valid_from <= CURRENT_TIMESTAMP(6)
              AND (e.valid_until IS NULL OR e.valid_until > CURRENT_TIMESTAMP(6))
            """, nativeQuery = true)
    long countActiveBySubModuleId(@Param("subModuleId") Long subModuleId);

    /**
     * Borrado fisico acotado por empresa: la primera mitad del recalculo. Nombra la
     * empresa en el {@code WHERE}, como exige
     * {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA}. No mueve ninguna {@code version}
     * porque esta tabla no la lleva --y porque un {@code DELETE} se lleva la fila,
     * no hay version que proteger--.
     *
     * <p>
     * <strong>Solo borra lo derivable.</strong> Una {@code MANUAL_GRANT} no sale de
     * ningun contrato, asi que el recalculo no sabria reconstruirla: borrarla la
     * haria desaparecer para siempre, y encima disparado por un cambio en otra
     * linea del contrato que nadie relacionaria con ella. El {@code <>} deja fuera
     * exactamente esas filas.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            DELETE FROM company_entitlements
            WHERE company_id = :companyId
              AND source <> 'MANUAL_GRANT'
            """, nativeQuery = true)
    int deleteDerivedByCompanyId(@Param("companyId") Long companyId);
}
