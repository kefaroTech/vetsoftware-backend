package com.vetsoftware.app.accountmapping.infrastructure.persistence;

import com.vetsoftware.app.accountmapping.domain.MappingKind;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * <strong>Sin una sola {@code @Query} de {@code UPDATE} ni de {@code DELETE}, y
 * no es que aun no hayan hecho falta.</strong> La unica escritura que edita una
 * fila es el cierre de la vigencia, y va por el ciclo leer-modificar-guardar de
 * una entidad gestionada, que es el unico camino que {@code @Version} protege
 * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}).
 *
 * <p>
 * Ningun metodo recibe {@code companyId} porque la tabla no tiene esa columna.
 */
public interface AccountMappingJpaRepository extends JpaRepository<AccountMappingJpaEntity, Long> {

    Page<AccountMappingJpaEntity> findAllByEnabledTrue(Pageable pageable);

    /**
     * El mapeo vigente para un supuesto en una fecha.
     *
     * <p>
     * <strong>Los tres afinados se comparan contra su clave con centinela y no
     * contra {@code NULL}</strong>, que es lo unico que hace encontrables a los
     * nueve tipos de mapeo que no llevan articulo. En SQL
     * {@code catalog_item_id = NULL} no es cierto ni siquiera para las filas que lo
     * tienen nulo, asi que un parametro nulo devolveria <b>cero filas</b>; el
     * asiento no se generaria y no habria ningun error que lo delatara. Los tres
     * {@code coalesce} reproducen exactamente lo que la base ya guarda en
     * {@code catalog_item_key}, {@code charge_type_key} y
     * {@code tax_treatment_key}, de modo que la consulta y el indice unico hablan
     * del mismo valor.
     *
     * <p>
     * <strong>El limite superior es estricto</strong> ({@code validTo > :on}): el
     * dia escrito en {@code valid_to} es el primero en que el mapeo ya no aplica.
     *
     * <p>
     * Devuelve {@code List} y no {@code Optional} <b>a proposito</b>: entre las
     * vigencias cerradas puede haber solape si alguien cargo mal el historico —la
     * unicidad de {@code current_mapping_marker} solo protege a las abiertas—, y un
     * {@code Optional} reventaria ahi con un {@code NonUniqueResultException} en
     * mitad de la generacion de un asiento. Con la lista mas el orden del
     * {@code Pageable}, el adaptador se queda con el mas reciente y la respuesta es
     * determinista.
     */
    @Query("""
            select m
            from AccountMappingJpaEntity m
            where m.enabled = true
              and m.mappingKind = :mappingKind
              and m.mappingKey = :mappingKey
              and coalesce(m.catalogItemId, 0L) = :catalogItemKey
              and coalesce(m.chargeType, '-') = :chargeTypeKey
              and coalesce(m.taxTreatment, '-') = :taxTreatmentKey
              and m.validFrom <= :on
              and (m.validTo is null or m.validTo > :on)
            """)
    List<AccountMappingJpaEntity> findEffective(@Param("mappingKind") MappingKind mappingKind,
            @Param("mappingKey") String mappingKey, @Param("catalogItemKey") Long catalogItemKey,
            @Param("chargeTypeKey") String chargeTypeKey,
            @Param("taxTreatmentKey") String taxTreatmentKey, @Param("on") LocalDate on,
            Pageable pageable);
}
