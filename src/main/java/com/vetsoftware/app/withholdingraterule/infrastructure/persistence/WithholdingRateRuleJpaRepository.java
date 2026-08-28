package com.vetsoftware.app.withholdingraterule.infrastructure.persistence;

import com.vetsoftware.app.withholdingraterule.domain.ServiceNature;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingType;
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
 * una entidad gestionada, que es el unico camino que {@code @Version} protege.
 * Un {@code UPDATE} masivo aqui pasaria de largo del bloqueo optimista y
 * dejaria la fila cambiada con su version intacta: el {@code save} concurrente
 * que llegara con la version vieja casaria igual y pisaria el cierre, sin
 * excepcion y sin log ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}).
 *
 * <p>
 * Ningun metodo recibe {@code companyId} porque la tabla no tiene esa columna:
 * es un catalogo global.
 */
public interface WithholdingRateRuleJpaRepository
        extends
            JpaRepository<WithholdingRateRuleJpaEntity, Long> {

    Page<WithholdingRateRuleJpaEntity> findAllByEnabledTrue(Pageable pageable);

    /**
     * La tarifa vigente para un supuesto en una fecha.
     *
     * <p>
     * <strong>El municipio se compara contra la clave con centinela y no contra
     * {@code NULL}</strong>, que es lo unico que hace encontrables a las
     * retenciones nacionales. En SQL {@code municipality_code = NULL} no es cierto
     * ni siquiera para las filas que lo tienen nulo, asi que un parametro nulo
     * devolveria <b>cero filas</b> para toda retencion nacional; la retencion
     * esperada saldria cero y no habria ningun error que lo delatara. El
     * {@code coalesce} reproduce exactamente lo que la base ya guarda en la columna
     * generada {@code municipality_key}, de modo que la consulta y el indice unico
     * hablan del mismo valor.
     *
     * <p>
     * <strong>El limite superior es estricto</strong> ({@code validTo > :on}): el
     * dia escrito en {@code valid_to} es el primero en que la tarifa ya no aplica,
     * asi que la regla que se cierra y la que la releva ese mismo dia se turnan sin
     * solaparse un dia entero.
     *
     * <p>
     * Devuelve {@code List} y no {@code Optional} <b>a proposito</b>: entre las
     * vigencias cerradas puede haber solape si alguien cargo mal el historico —la
     * unicidad de {@code current_rule_marker} solo protege a las abiertas—, y un
     * {@code Optional} reventaria ahi con un {@code NonUniqueResultException} en
     * mitad del calculo de una factura. Con la lista mas el orden del
     * {@code Pageable}, el adaptador se queda con la mas reciente y la respuesta es
     * determinista.
     */
    @Query("""
            select r
            from WithholdingRateRuleJpaEntity r
            where r.enabled = true
              and r.withholdingType = :withholdingType
              and r.serviceNature = :serviceNature
              and coalesce(r.municipalityCode, '-') = :municipalityKey
              and r.validFrom <= :on
              and (r.validTo is null or r.validTo > :on)
            """)
    List<WithholdingRateRuleJpaEntity> findEffective(
            @Param("withholdingType") WithholdingType withholdingType,
            @Param("serviceNature") ServiceNature serviceNature,
            @Param("municipalityKey") String municipalityKey, @Param("on") LocalDate on,
            Pageable pageable);
}
