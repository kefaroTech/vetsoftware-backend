package com.vetsoftware.app.accountingperiod.application.port.out;

import com.vetsoftware.app.accountingperiod.domain.AccountingPeriod;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodKey;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;

/**
 * <strong>No hay variante acotada por empresa de nada, y no falta
 * ninguna.</strong> La tabla no tiene {@code company_id}: no existe la consulta
 * acotada que {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} echaria de menos, ni el
 * filtro que {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} exigiria. Lo que sostiene
 * el aislamiento aqui no es un {@code WHERE}: es que los siete puertos de
 * entrada de la feature estan cerrados a {@code hasRole('SYSTEM')} a secas.
 *
 * <p>
 * <strong>Y no hay borrado, ni logico ni fisico.</strong> Ni {@code delete}, ni
 * {@code disable}, ni reactivacion. Un mes que existio no deja de existir, y la
 * fila es ademas el destino de la clave foranea {@code fk_eir_posting_period}:
 * borrarla dejaria conciliaciones imputadas a un mes inexistente —o, con el
 * {@code RESTRICT} que la migracion declara, ni siquiera dejaria borrarla—.
 */
public interface AccountingPeriodRepository {

    AccountingPeriod save(AccountingPeriod period);

    Optional<AccountingPeriod> findById(Long id);

    /**
     * Si ese mes ya se abrio.
     *
     * <p>
     * Se consulta <strong>antes</strong> de insertar porque
     * {@code uq_accounting_periods_period} convierte el duplicado en un error del
     * driver, y abrir dos veces el mismo mes —el cierre mensual lanzado a mano y
     * ademas por el programador de tareas— merece un conflicto legible y no un 500.
     * La unicidad de la base sigue siendo lo unico que <em>garantiza</em> que no
     * entren dos: entre el {@code exists} y el {@code insert} cabe otra
     * transaccion.
     */
    boolean existsByPeriodKey(AccountingPeriodKey periodKey);

    /**
     * Cuantos periodos {@code OPEN} hay <strong>aparte del indicado</strong>.
     *
     * <p>
     * Es la consulta que sostiene «tiene que existir siempre al menos un periodo
     * abierto». Se excluye el propio periodo —y no se resta uno al total— porque el
     * que se esta cerrando puede no estar abierto: restar a ciegas daria un cero
     * falso al declarar un mes ya cerrado y bloquearia una operacion legitima.
     */
    long countOpenExcluding(Long excludedId);

    /**
     * El primer mes <strong>abierto</strong> cuya clave sea mayor o igual que la
     * dada, en orden ascendente.
     *
     * <p>
     * Las dos ramas de la regla de imputacion caben en esta unica consulta: si el
     * mes de la fecha esta abierto, su clave es la primera que cumple {@code >=} y
     * se devuelve ese; si no lo esta, la primera que cumple es la del siguiente mes
     * abierto. Que el {@code >=} lexicografico coincida con el cronologico es
     * propiedad del formato {@code yyyy-MM} y esta explicado en
     * {@link AccountingPeriodKey}.
     */
    Optional<AccountingPeriod> findFirstOpenFrom(AccountingPeriodKey periodKey);

    /** El calendario completo. Solo lo consume un puerto SYSTEM. */
    PageResult<AccountingPeriod> findAll(int page, int pageSize);
}
