package com.vetsoftware.app.companybillingprofile.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * <strong>Las tres consultas llevan {@code @EntityGraph} sobre
 * {@code city}</strong>, que es obligatorio con la asociacion {@code LAZY}: sin
 * el, listar diez fichas del historico dispara once consultas y el N+1 no se ve
 * hasta que el historico crece.
 *
 * <p>
 * <strong>No se sobrescribe {@code findAll()} y no es un olvido.</strong> Esta
 * feature nunca lista sin empresa —la ficha dice a quien se le cobra y con que
 * documento—, asi que el puerto de salida no declara la variante ancha y el
 * adaptador no la llama. Añadirla aqui «por simetria» pondria a disposicion de
 * cualquiera un barrido de todos los tenants: exactamente lo que persigue
 * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}.
 *
 * <p>
 * <strong>Sin una sola {@code @Query} de {@code UPDATE} o
 * {@code DELETE}.</strong> Toda escritura pasa por el ciclo
 * leer-modificar-guardar, que es el unico camino que {@code @Version} protege,
 * asi que aqui no hay SQL que pudiera olvidarse de mover la version en su
 * {@code SET} ({@code #53}) ni al que
 * {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA} tuviera que pedirle un filtro.
 */
public interface CompanyBillingProfileJpaRepository
        extends
            JpaRepository<CompanyBillingProfileJpaEntity, Long> {

    /**
     * <strong>Acotada por empresa, y no hay hermana ancha.</strong> El
     * {@code findById} heredado de {@code JpaRepository} sigue existiendo por
     * herencia, pero ni el puerto de salida lo declara ni el adaptador lo llama:
     * quien quisiera usarlo tendria que escribirlo, y eso se ve en el diff.
     */
    @EntityGraph(attributePaths = "city")
    @Query("""
            SELECT p
            FROM CompanyBillingProfileJpaEntity p
            WHERE p.id = :id
              AND p.companyId = :companyId
            """)
    Optional<CompanyBillingProfileJpaEntity> findByIdAndCompanyId(@Param("id") Long id,
            @Param("companyId") Long companyId);

    /**
     * La ficha vigente: {@code valid_to} nulo. Recorre
     * {@code ix_company_billing_profiles_current (company_id, valid_to)} en el
     * mismo orden en que el indice esta escrito.
     *
     * <p>
     * Devuelve {@code Optional} y no {@code List} porque la unicidad no la sostiene
     * esta consulta: la impone {@code uq_company_billing_profiles_current} sobre la
     * columna generada. Si alguna vez llegaran dos, lo correcto es que Spring Data
     * lance en vez de que el servicio elija una en silencio.
     */
    @EntityGraph(attributePaths = "city")
    @Query("""
            SELECT p
            FROM CompanyBillingProfileJpaEntity p
            WHERE p.companyId = :companyId
              AND p.validTo IS NULL
            """)
    Optional<CompanyBillingProfileJpaEntity> findCurrentByCompanyId(
            @Param("companyId") Long companyId);

    @EntityGraph(attributePaths = "city")
    Page<CompanyBillingProfileJpaEntity> findAllByCompanyId(Long companyId, Pageable pageable);
}
