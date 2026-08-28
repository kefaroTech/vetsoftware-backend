package com.vetsoftware.app.accountingperiod.testsupport;

import com.vetsoftware.app.accountingperiod.domain.AccountingPeriod;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodKey;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodStatus;
import java.time.LocalDateTime;

/**
 * Periodos contables listos para usar.
 *
 * <p>
 * <b>Los tres instantes son deliberadamente distintos entre si</b> —creacion,
 * cierre y reapertura— y ademas estan en ese orden, para que cruzar dos
 * columnas en un mapper o en un command haga caer la asercion. Con el mismo
 * valor en los tres, no caeria.
 *
 * <p>
 * <b>Y los dos firmantes tambien son distintos</b>: quien cierra no es quien
 * reabre. Es el escenario real —una reapertura la pide alguien distinto de
 * quien cerro— y es lo que hace visible en la asercion si las dos columnas de
 * firma se cruzan.
 */
public final class AccountingPeriodMother {

    public static final AccountingPeriodKey MARZO = AccountingPeriodKey.of("2026-03");
    public static final AccountingPeriodKey ABRIL = AccountingPeriodKey.of("2026-04");

    public static final LocalDateTime CREADO_EL = LocalDateTime.of(2026, 3, 1, 0, 5, 0);
    public static final LocalDateTime CERRADO_EL = LocalDateTime.of(2026, 4, 5, 17, 30, 15);
    public static final LocalDateTime REABIERTO_EL = LocalDateTime.of(2026, 4, 9, 9, 12, 45);

    public static final Long CERRADO_POR = 6L;
    public static final Long REABIERTO_POR = 11L;

    public static final String MOTIVO = "Ajuste de la conciliacion 4471, recibida fuera de plazo";

    private AccountingPeriodMother() {
    }

    /** Mes recien abierto, todavia sin persistir. */
    public static AccountingPeriod abierto() {
        return AccountingPeriod.open(MARZO, CREADO_EL);
    }

    /**
     * Ya persistido: con id y con version, que es lo que ve un {@code findById}.
     */
    public static AccountingPeriod persistidoAbierto(Long id) {
        return new AccountingPeriod(id, MARZO, AccountingPeriodStatus.OPEN, null, null, null, null,
                null, CREADO_EL, 0L);
    }

    /** Cerrado en blando: con las dos columnas de cierre, sin reapertura. */
    public static AccountingPeriod cerradoEnBlando(Long id) {
        return new AccountingPeriod(id, MARZO, AccountingPeriodStatus.SOFT_CLOSED, CERRADO_EL,
                CERRADO_POR, null, null, null, CREADO_EL, 0L);
    }

    /** Declarado: mismo cierre, estado terminal. */
    public static AccountingPeriod declarado(Long id) {
        return new AccountingPeriod(id, MARZO, AccountingPeriodStatus.LOCKED, CERRADO_EL,
                CERRADO_POR, null, null, null, CREADO_EL, 0L);
    }

    /**
     * Reabierto: {@code OPEN} y <b>conservando</b> el cierre previo.
     *
     * <p>
     * <b>Es la forma que hoy NO cabe en la base</b>: las dos {@code CHECK} de la
     * migracion 331 se excluyen para esta combinacion. El dominio la construye
     * porque es la unica que tiene sentido de negocio; que el motor la rechace lo
     * congela {@code AccountingPeriodPersistenceIT}.
     */
    public static AccountingPeriod reabierto(Long id) {
        return new AccountingPeriod(id, MARZO, AccountingPeriodStatus.OPEN, CERRADO_EL, CERRADO_POR,
                REABIERTO_EL, REABIERTO_POR, MOTIVO, CREADO_EL, 0L);
    }

    /** Mes abierto con la clave que se le pida. */
    public static AccountingPeriod abiertoCon(AccountingPeriodKey clave) {
        return AccountingPeriod.open(clave, CREADO_EL);
    }
}
