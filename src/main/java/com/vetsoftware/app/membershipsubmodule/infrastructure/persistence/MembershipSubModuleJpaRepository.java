package com.vetsoftware.app.membershipsubmodule.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipSubModuleJpaRepository
        extends
            JpaRepository<MembershipSubModuleJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"membership", "subModule"})
    List<MembershipSubModuleJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"membership", "subModule"})
    Optional<MembershipSubModuleJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "subModule")
    List<MembershipSubModuleJpaEntity> findByMembershipId(Long membershipId);

    @EntityGraph(attributePaths = {"membership", "subModule"})
    List<MembershipSubModuleJpaEntity> findByMembershipIdIn(Collection<Long> membershipIds);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE membership_sub_modules
            SET enabled = true
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT id
            FROM membership_sub_modules
            WHERE membership_id = :membershipId
              AND sub_module_id = :subModuleId
              AND enabled = false
            LIMIT 1
            """, nativeQuery = true)
    Optional<Long> findDisabledIdByMembershipAndSubModule(
            @org.springframework.data.repository.query.Param("membershipId") Long membershipId,
            @org.springframework.data.repository.query.Param("subModuleId") Long subModuleId);

    boolean existsByMembership_Id(Long membershipId);

    boolean existsBySubModule_Id(Long subModuleId);

    /**
     * ¿La membresía tiene habilitado (enabled) el submódulo con este código? Filtra
     * enabled=true de forma explícita en ambos lados (el enlace y el submódulo) —
     * no depende solo de @SQLRestriction. Base del gating de facturación
     * electrónica por membresía.
     *
     * <p>
     * <b>Por qué es una consulta derivada y no una {@code @Query}.</b> Hasta la
     * incidencia #185 esto era un JPQL con la forma
     * {@code SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END}. Con Hibernate
     * 7 esa expresión se tipa como {@code Integer} al extraer el resultado, así que
     * la coerción del literal {@code Boolean} lanza
     * {@code Cannot coerce value 'true' [java.lang.Boolean] to Integer} — el 100 %
     * de las veces, no un porcentaje.
     *
     * <p>
     * El daño no se quedaba aquí: esta consulta es el cuerpo entero de
     * {@code JpaBillingEntitlementQueryPort.isElectronicInvoicingEnabled}, que es
     * la <b>primera</b> lectura a base de datos de toda emisión, transmisión,
     * reconciliación y webhook DIAN. La facturación electrónica quedó caída al 100
     * % sin que ninguna prueba lo viera, porque su único uso en el árbol de test
     * era un mock. La derivada la traduce Spring Data a una proyección de
     * existencia y no depende de ninguna inferencia de tipos sobre un literal;
     * {@code MembershipSubModulePersistenceIT} la ejecuta contra MySQL real.
     */
    boolean existsByMembership_IdAndSubModule_CodeAndEnabledTrueAndSubModule_EnabledTrue(
            Long membershipId, String code);

    /**
     * Nombre de negocio del gating de facturación electrónica: delega en la
     * derivada de arriba para que los llamadores no arrastren su nombre generado.
     */
    default boolean hasEnabledSubModuleCode(Long membershipId, String code) {
        return existsByMembership_IdAndSubModule_CodeAndEnabledTrueAndSubModule_EnabledTrue(
                membershipId, code);
    }
}
