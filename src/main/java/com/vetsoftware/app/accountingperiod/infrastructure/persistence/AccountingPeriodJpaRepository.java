package com.vetsoftware.app.accountingperiod.infrastructure.persistence;

import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * <strong>Sin una sola {@code @Query} de escritura.</strong> Las cuatro
 * consultas propias de la feature las expresa el derivador de nombres de Spring
 * Data —la unica {@code @Query} es la de solo lectura que consulta
 * {@code revenuerecognitionline}, abajo—, asi que aqui no hay SQL que pueda
 * olvidarse de mover la {@code version} en su {@code SET}
 * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}, #53) ni
 * {@code UPDATE}/{@code DELETE} al que
 * {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA} pudiera pedirle un filtro de
 * empresa que la tabla no tiene. Toda escritura pasa por el ciclo
 * leer-modificar-guardar, que es el unico camino que {@code @Version} protege.
 *
 * <p>
 * <strong>Sin {@code @EntityGraph}</strong>, y no por descuido: la entidad no
 * tiene ni una asociacion —las dos firmas son ids escalares— asi que no hay N+1
 * que evitar.
 */
public interface AccountingPeriodJpaRepository
        extends
            JpaRepository<AccountingPeriodJpaEntity, Long> {

    /**
     * <strong>La igualdad la resuelve el motor bajo la colacion de la columna, que
     * es {@code ascii_bin}</strong> — es decir, byte a byte. Sobre una clave de mes
     * eso no cambia nada respecto de una colacion insensible, porque
     * {@code yyyy-MM} solo tiene digitos y un guion; lo que si cambia es que esta
     * comparacion es exactamente la misma que hace
     * {@code uq_accounting_periods_period}, asi que el {@code exists} y la unicidad
     * no pueden discrepar.
     */
    boolean existsByPeriodKey(String periodKey);

    /**
     * Cuantos periodos hay en ese estado <strong>que no sean el indicado</strong>.
     * Es la consulta de la invariante «siempre al menos un periodo abierto»: el que
     * se esta cerrando se excluye por id porque todavia figura abierto en la base
     * cuando se pregunta.
     */
    long countByStatusAndIdNot(AccountingPeriodStatus status, Long id);

    /**
     * El primer periodo en ese estado con clave mayor o igual que la dada.
     *
     * <p>
     * <strong>El {@code Sort} llega como parametro y no como
     * {@code OrderByPeriodKeyAsc} en el nombre</strong>, que es lo que exige el
     * CLAUDE.md: el orden lo decide el adaptador, y ademas tiene que ser total. Sin
     * el desempate por {@code id} el orden de dos filas con la misma clave lo
     * elegiria el motor — imposible hoy, porque {@code period_key} es unica, pero
     * lo que sostiene esa unicidad es una constraint que un changeset futuro puede
     * mover, y este metodo devuelve <em>una</em> fila: la equivocada seria el mes
     * equivocado, sin error.
     */
    Optional<AccountingPeriodJpaEntity> findFirstByStatusAndPeriodKeyGreaterThanEqual(
            AccountingPeriodStatus status, String periodKey, Sort sort);

    /**
     * Las claves de los periodos <b>abiertos</b> a partir de la dada, en orden
     * cronologico.
     *
     * <p>
     * <strong>Este metodo no lo usa esta feature: lo usa
     * {@code revenuerecognitionline}</strong>, para resolver «un hecho tardio se
     * reconoce en el primer periodo abierto» (§6.4 de la especificacion del bloque
     * contable), que es la unica de las cuatro reglas de periodo que ni un
     * {@code CHECK} ni una clave foranea pueden imponer.
     *
     * <p>
     * <strong>Vive aqui y no en un adaptador de aquel slice por la misma
     * coordinacion que documenta {@code JpaMunicipalityValidationPort}</strong>: el
     * {@code XxxJpaRepository} de una feature se declara una sola vez. Y proyecta
     * la <em>clave</em> en vez de devolver la entidad justamente para que el otro
     * slice no tenga que importar {@code AccountingPeriodStatus} —el dominio de
     * esta feature—, que es lo que el vertical slicing prohibe. El literal del enum
     * va cualificado dentro del JPQL, que es donde si puede estar.
     *
     * <p>
     * El {@code >=} sobre {@code CHAR(7) ascii_bin} ordena como el calendario
     * porque el formato es {@code AAAA-MM}. El desempate por {@code id} no es
     * adorno: {@code uq_accounting_periods_period} lo hace imposible hoy, pero un
     * orden que depende de una constraint que un changeset futuro puede mover
     * devolveria el mes equivocado sin ningun error.
     */
    @Query("""
            select p.periodKey
            from AccountingPeriodJpaEntity p
            where p.status = com.vetsoftware.app.accountingperiod.domain.AccountingPeriodStatus.OPEN
              and p.periodKey >= :periodKey
            order by p.periodKey asc, p.id asc
            """)
    List<String> findOpenPeriodKeysFrom(@Param("periodKey") String periodKey, Pageable pageable);
}
