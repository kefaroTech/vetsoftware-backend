package com.vetsoftware.app.accountingperiod.domain;

import java.time.LocalDateTime;

/**
 * Un mes contable y su estado de cierre: la pieza que impide que un hecho
 * tardio reescriba un mes ya declarado.
 *
 * <p>
 * <strong>Sin empresa, y no por descuido.</strong> La tabla no tiene
 * {@code company_id} porque el calendario contable es de la plataforma y no de
 * cada clinica: si cada tenant cerrara sus meses por su cuenta, la misma
 * conciliacion podria estar imputada a marzo para uno y a abril para otro. Lo
 * que sostiene el aislamiento aqui no es un {@code WHERE}, es que los siete
 * puertos de entrada estan cerrados a {@code hasRole('SYSTEM')} a secas.
 *
 * <p>
 * <strong>Sin {@code enabled} y sin borrado, ni logico ni fisico.</strong> Un
 * mes que existio no deja de existir. Un cierre no se desactiva: se reabre, y
 * queda constancia de quien lo hizo y por que.
 *
 * <p>
 * <strong>Con {@code version}</strong>, y es de las pocas fichas donde el
 * bloqueo optimista protege algo que no se puede rehacer: el estado muta dos
 * veces declaradamente —cierre y reapertura— y dos personas atendiendo el mismo
 * cierre mensual se pisarian sin ruido.
 *
 * <h2>Las dos reglas de negocio que NO viven en esta clase, y por que</h2>
 *
 * <ul>
 * <li><strong>«Tiene que existir siempre al menos un periodo abierto»</strong>
 * la comprueba {@code SoftCloseAccountingPeriodService} contando los demas
 * {@code OPEN}. Un agregado solo puede hablar de si mismo; aqui la pregunta es
 * cuantos hermanos le quedan.</li>
 * <li><strong>«Un hecho tardio se registra en el primer periodo abierto, nunca
 * hacia atras»</strong> la resuelve {@code ResolvePostingPeriodUseCase}, porque
 * es una consulta sobre el conjunto de periodos. Lo que esta clase aporta a esa
 * regla es {@link #acceptsPostings()}.</li>
 * </ul>
 *
 * <h2>Donde este dominio se separa del esquema, a proposito</h2>
 *
 * <p>
 * <strong>Las dos {@code CHECK} de la migracion 331 hacen hoy imposible
 * persistir un periodo reabierto.</strong> Reabrir deja el estado en
 * {@code OPEN}, y {@code chk_accounting_periods_closure} exige que un
 * {@code OPEN} tenga {@code closed_at} nulo; pero
 * {@code chk_accounting_periods_reopening} exige {@code closed_at IS NOT NULL}
 * para poder escribir {@code reopened_at}. Las dos ramas se excluyen: no existe
 * fila que satisfaga las dos.
 *
 * <p>
 * Este dominio implementa la <strong>regla de negocio</strong>, que es la
 * correcta —un periodo reabierto conserva el cierre previo como registro de que
 * ocurrio—, y {@link #validateClosure} lo dice explicitamente: la unica
 * divergencia con la constraint es que un {@code OPEN} <em>reabierto</em> si
 * lleva cierre. {@code AccountingPeriodPersistenceIT} congela por su nombre que
 * el motor hoy lo rechaza, para que el dia que el changeset se corrija ese caso
 * se ponga rojo y se invierta.
 *
 * <p>
 * <strong>Hay una segunda cara del mismo defecto, mas dificil de ver.</strong>
 * Un periodo que se cierra, se reabre y se vuelve a cerrar queda con
 * {@code reopened_at &lt; closed_at}, que la segunda constraint tampoco admite
 * —y que como regla permanente <em>no</em> deberia exigirse: «no puedes reabrir
 * antes de haber cerrado» es una condicion del instante de la reapertura, no
 * una propiedad eterna de la fila—. Por eso {@link #reopen} la comprueba al
 * reabrir y {@link #softClose} no la vuelve a mirar.
 */
public class AccountingPeriod {

    /** {@code reopened_reason VARCHAR(255)}. */
    private static final int MAX_REOPENED_REASON_LENGTH = 255;

    private final Long id;

    /** El mes, {@code yyyy-MM}. Nunca cambia: es la identidad del periodo. */
    private final AccountingPeriodKey periodKey;

    private AccountingPeriodStatus status;

    /** Cuando se cerro. Sobrevive a la reapertura: es el registro del cierre. */
    private LocalDateTime closedAt;

    /**
     * Quien cerro, como columna escalar y <strong>no</strong> como
     * {@code @ManyToOne}. Es el precedente vivo del repositorio
     * ({@code price_lists.published_by_system_user_id},
     * {@code company_limit_overrides}, {@code subscription_billing_documents}): de
     * la firma solo hace falta que exista, y colgar una asociacion traeria un
     * agregado ajeno a un dominio que no lo necesita.
     */
    private Long closedBySystemUserId;

    private LocalDateTime reopenedAt;
    private Long reopenedBySystemUserId;

    /**
     * Obligatorio cuando hay reapertura: un cierre que cualquiera deshace sin decir
     * por que no significa nada.
     */
    private String reopenedReason;

    private final LocalDateTime createdDate;
    private final Long version;

    public AccountingPeriod(Long id, AccountingPeriodKey periodKey, AccountingPeriodStatus status,
            LocalDateTime closedAt, Long closedBySystemUserId, LocalDateTime reopenedAt,
            Long reopenedBySystemUserId, String reopenedReason, LocalDateTime createdDate,
            Long version) {
        validate(periodKey, status, closedAt, closedBySystemUserId, reopenedAt,
                reopenedBySystemUserId, reopenedReason);
        this.id = id;
        this.periodKey = periodKey;
        this.status = status;
        this.closedAt = closedAt;
        this.closedBySystemUserId = closedBySystemUserId;
        this.reopenedAt = reopenedAt;
        this.reopenedBySystemUserId = reopenedBySystemUserId;
        this.reopenedReason = reopenedReason;
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * Mes recien abierto: nace {@link AccountingPeriodStatus#OPEN} y sin cierre ni
     * reapertura, que es la unica combinacion que la base admite para ese estado.
     */
    public static AccountingPeriod open(AccountingPeriodKey periodKey, LocalDateTime createdDate) {
        return new AccountingPeriod(null, periodKey, AccountingPeriodStatus.OPEN, null, null, null,
                null, null, createdDate, null);
    }

    /**
     * Cierra el mes dejandolo corregible: {@code OPEN} → {@code SOFT_CLOSED}.
     *
     * @param closedAt
     *            del reloj inyectado del caso de uso, nunca de un
     *            {@code LocalDateTime.now()} pelado: la JVM corre en UTC y la zona
     *            del negocio es {@code America/Bogota}, asi que un cierre lanzado a
     *            las 19:30 del ultimo dia del mes quedaria fechado en el mes
     *            siguiente — justo el dato que decide si el cierre llego a tiempo
     */
    public void softClose(Long systemUserId, LocalDateTime closedAt) {
        if (status != AccountingPeriodStatus.OPEN)
            throw new AccountingPeriodAlreadyClosedException(id, status);
        seal(AccountingPeriodStatus.SOFT_CLOSED, systemUserId, closedAt);
    }

    /**
     * Declara el mes: pasa a {@link AccountingPeriodStatus#LOCKED} y ya no se toca.
     *
     * <p>
     * <strong>Acepta las dos entradas, y la de {@code SOFT_CLOSED} no vuelve a
     * sellar.</strong> Desde {@code OPEN} se declara y se cierra en un solo acto
     * —hay meses que se declaran sin pasar por la revision—, y ahi si se escribe el
     * cierre. Desde {@code SOFT_CLOSED} el cierre ya ocurrio y sus dos columnas se
     * conservan: la tabla guarda <em>un</em> cierre, no una pila, asi que
     * sobrescribirlas borraria quien cerro el mes para poner quien lo declaro.
     */
    public void lock(Long systemUserId, LocalDateTime closedAt) {
        if (status == AccountingPeriodStatus.LOCKED)
            throw new AccountingPeriodAlreadyClosedException(id, status);
        if (status == AccountingPeriodStatus.OPEN) {
            seal(AccountingPeriodStatus.LOCKED, systemUserId, closedAt);
            return;
        }
        this.status = AccountingPeriodStatus.LOCKED;
    }

    /**
     * Vuelve a abrir un mes cerrado, con firma y motivo escrito.
     *
     * <p>
     * <strong>Solo desde {@code SOFT_CLOSED}.</strong> Un {@code LOCKED} esta
     * declarado y no se reabre nunca
     * ({@link LockedAccountingPeriodCannotBeReopenedException}); un {@code OPEN} no
     * tiene nada que reabrir ({@link AccountingPeriodNotClosedException}).
     *
     * <p>
     * <strong>Conserva {@code closedAt} y {@code closedBySystemUserId}</strong>:
     * son el registro de que el mes llego a estar cerrado, que es exactamente lo
     * que un revisor mira primero. Borrarlos dejaria una reapertura indistinguible
     * de un mes que nunca se cerro, y con ella la firma del cierre que se deshizo.
     *
     * @param reason
     *            obligatorio y no en blanco. Es la operacion sobre la que un
     *            auditor pregunta primero, y {@code reopened_reason} es donde vive
     *            la respuesta
     */
    public void reopen(Long systemUserId, LocalDateTime reopenedAt, String reason) {
        if (status == AccountingPeriodStatus.LOCKED)
            throw new LockedAccountingPeriodCannotBeReopenedException(id);
        if (status != AccountingPeriodStatus.SOFT_CLOSED)
            throw new AccountingPeriodNotClosedException(id, status);
        if (systemUserId == null)
            throw new IllegalArgumentException("reopenedBySystemUserId is required");
        if (reopenedAt == null)
            throw new IllegalArgumentException("reopenedAt is required");
        validateReason(reason);
        if (reopenedAt.isBefore(closedAt))
            throw new IllegalArgumentException("reopenedAt cannot be before closedAt");
        this.status = AccountingPeriodStatus.OPEN;
        this.reopenedAt = reopenedAt;
        this.reopenedBySystemUserId = systemUserId;
        this.reopenedReason = reason;
    }

    /**
     * <strong>La regla «un periodo cerrado no admite escrituras», escrita como
     * codigo porque la base no puede escribirla.</strong> Un {@code INSERT} con
     * fecha de marzo altera marzo igual que editar una fila de marzo, y ninguna
     * constraint de fila puede saber si marzo esta abierto en el momento de
     * insertar. Quien registre un hecho con efecto contable resuelve su periodo con
     * {@code ResolvePostingPeriodUseCase}, que nunca devuelve uno que conteste
     * {@code false} aqui.
     */
    public boolean acceptsPostings() {
        return status == AccountingPeriodStatus.OPEN;
    }

    /** Si en algun momento se reabrio. Un periodo reabierto conserva su cierre. */
    public boolean isReopened() {
        return reopenedAt != null;
    }

    private void seal(AccountingPeriodStatus target, Long systemUserId, LocalDateTime closedAt) {
        if (systemUserId == null)
            throw new IllegalArgumentException("closedBySystemUserId is required");
        if (closedAt == null)
            throw new IllegalArgumentException("closedAt is required");
        this.status = target;
        this.closedAt = closedAt;
        this.closedBySystemUserId = systemUserId;
    }

    private static void validate(AccountingPeriodKey periodKey, AccountingPeriodStatus status,
            LocalDateTime closedAt, Long closedBySystemUserId, LocalDateTime reopenedAt,
            Long reopenedBySystemUserId, String reopenedReason) {
        if (periodKey == null)
            throw new IllegalArgumentException("periodKey is required");
        if (status == null)
            throw new IllegalArgumentException("status is required");
        validateClosure(status, closedAt, closedBySystemUserId, reopenedAt);
        validateReopening(closedAt, reopenedAt, reopenedBySystemUserId, reopenedReason);
    }

    /**
     * Espejo de {@code chk_accounting_periods_closure}, con <strong>una</strong>
     * divergencia declarada.
     *
     * <p>
     * La constraint dice: {@code OPEN} exige las dos columnas de cierre nulas, y
     * {@code SOFT_CLOSED}/{@code LOCKED} las exigen las dos presentes. Las dos
     * mitades importan — sin la primera, un mes abierto aparenta un cierre que no
     * ocurrio; sin la segunda, un mes cerrado pierde quien lo cerro, que es el dato
     * por el que se pregunta.
     *
     * <p>
     * <strong>La divergencia es el periodo reabierto</strong>, que esta
     * {@code OPEN} y <em>si</em> conserva su cierre. No es una relajacion
     * discrecional: es que la constraint, tal como esta escrita hoy, hace imposible
     * cualquier fila reabierta (ver el javadoc de la clase). Se admite aqui la
     * unica forma que tiene sentido de negocio y se documenta arriba que el motor
     * todavia la rechaza.
     */
    private static void validateClosure(AccountingPeriodStatus status, LocalDateTime closedAt,
            Long closedBySystemUserId, LocalDateTime reopenedAt) {
        if ((closedAt == null) != (closedBySystemUserId == null))
            throw new IllegalArgumentException(
                    "closedAt and closedBySystemUserId must be set together");
        if (status == AccountingPeriodStatus.OPEN && reopenedAt == null && closedAt != null)
            throw new IllegalArgumentException("an open period cannot carry a closure");
        if (status != AccountingPeriodStatus.OPEN && closedAt == null)
            throw new IllegalArgumentException("closedAt is required once closed");
    }

    /**
     * Espejo <strong>literal</strong> de {@code chk_accounting_periods_reopening},
     * con sus cuatro condiciones: o los tres campos de reapertura estan los tres
     * nulos, o estan los tres presentes <em>y ademas</em> hay un cierre previo y la
     * reapertura no es anterior a el.
     *
     * <p>
     * La ultima es la que parece redundante y no lo es: sin ella, una reapertura
     * fechada antes del cierre pasaria por buena y el orden de los hechos —lo unico
     * que un auditor puede reconstruir de esta ficha— quedaria invertido.
     */
    private static void validateReopening(LocalDateTime closedAt, LocalDateTime reopenedAt,
            Long reopenedBySystemUserId, String reopenedReason) {
        boolean alguno = reopenedAt != null || reopenedBySystemUserId != null
                || reopenedReason != null;
        boolean todos = reopenedAt != null && reopenedBySystemUserId != null
                && reopenedReason != null;
        if (!alguno)
            return;
        if (!todos)
            throw new IllegalArgumentException("reopenedAt, reopenedBySystemUserId and"
                    + " reopenedReason must be set together");
        validateReason(reopenedReason);
        if (closedAt == null)
            throw new IllegalArgumentException("a reopened period must keep its previous closure");
        if (reopenedAt.isBefore(closedAt))
            throw new IllegalArgumentException("reopenedAt cannot be before closedAt");
    }

    private static void validateReason(String reason) {
        if (reason == null || reason.isBlank())
            throw new IllegalArgumentException("reopenedReason is required");
        if (reason.length() > MAX_REOPENED_REASON_LENGTH)
            throw new IllegalArgumentException("reopenedReason must be 255 chars or less");
    }

    public Long getId() {
        return id;
    }

    public AccountingPeriodKey getPeriodKey() {
        return periodKey;
    }

    public AccountingPeriodStatus getStatus() {
        return status;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public Long getClosedBySystemUserId() {
        return closedBySystemUserId;
    }

    public LocalDateTime getReopenedAt() {
        return reopenedAt;
    }

    public Long getReopenedBySystemUserId() {
        return reopenedBySystemUserId;
    }

    public String getReopenedReason() {
        return reopenedReason;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
