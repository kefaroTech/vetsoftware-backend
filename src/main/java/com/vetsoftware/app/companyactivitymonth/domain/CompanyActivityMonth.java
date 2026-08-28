package com.vetsoftware.app.companyactivitymonth.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Que hizo una clinica en un mes: cuantos dias entro, cuanta gente suya la uso,
 * cuanto registro y cuanto pagaba.
 *
 * <p>
 * Es la tabla que hace que <b>«una clinica que entra veinte dias al mes» y
 * «otra que no entra ninguno» dejen de ser identicas en los informes</b>. Sin
 * ella las dos son «un cliente activo» y la que se esta yendo no se distingue
 * de la que se queda hasta que cancela.
 *
 * <h2>La forma de escritura es lo que define esta tabla</h2>
 *
 * <p>
 * <strong>El mes en curso se recalcula sobre si mismo cada dia hasta que
 * termina.</strong> No es una bitacora de solo agregar: la fila de
 * {@code 2026-08} nace el primer dia del mes y se reescribe cada noche con los
 * numeros acumulados, hasta que el mes cierra y deja de moverse. Por eso lleva
 * {@code version} y por eso la lleva desde su primer changeset: dos recalculos
 * concurrentes —el proceso nocturno y una correccion a mano, o dos reintentos
 * del mismo proceso— se pisarian y la serie quedaria con un valor arbitrario,
 * <b>sin excepcion y sin log</b>. Esa es exactamente la forma silenciosa de
 * fallo que {@code ENTIDADES_CON_BLOQUEO_OPTIMISTA} existe para cerrar.
 *
 * <p>
 * <strong>Y por eso el recalculo va por el ciclo
 * leer-modificar-guardar</strong> de una entidad gestionada, nunca por una
 * {@code @Query} de {@code UPDATE}: esa iria directa a la base, ni comprobaria
 * ni incrementaria la version, y el {@code save} concurrente que llegara con la
 * version vieja casaria igual y pisaria el recalculo
 * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}, incidencia #53).
 *
 * <p>
 * <strong>Una columna de «ultimo acceso» habria perdido la serie.</strong> Es
 * el mismo defecto que el modelo acaba de corregir en el ciclo de facturacion:
 * se sobrescribe, y con ella se va la historia. Aqui cada mes es una fila que
 * se congela cuando el mes acaba, asi que el pasado se puede leer sin
 * recalcularlo.
 *
 * <h2>Sin {@code enabled}, a proposito</h2>
 *
 * <p>
 * Una medicion no se desactiva. Poder ocultar un mes flojo del informe de
 * actividad seria poder maquillar la unica serie que dice si un cliente se esta
 * yendo.
 *
 * <h2>Los contadores no se derivan unos de otros</h2>
 *
 * <p>
 * {@code activeDays}, {@code activeUsers} y {@code recordsCreated} son tres
 * medidas independientes y ninguna implica a las otras: una clinica puede
 * entrar los treinta dias sin crear un solo registro (consulta de historiales),
 * y puede crear doscientos registros en un solo dia. El dominio <b>no</b>
 * comprueba coherencia cruzada entre ellas porque cualquier regla de ese tipo
 * seria una suposicion sobre el negocio, no una invariante.
 */
public class CompanyActivityMonth {

    /**
     * Espejo de {@code chk_cam_active_days}: {@code BETWEEN 0 AND 31}. El techo
     * real de cada mes lo comprueba ademas
     * {@link ActivityPeriodKey#lengthOfMonth()}, que el {@code CHECK} no puede
     * mirar sin funciones de calendario.
     */
    private static final int MAX_ACTIVE_DAYS = 31;

    /**
     * {@code DECIMAL(19,2)}. Una escala mayor no cabe: MySQL la redondearia sin
     * avisar y el MRR guardado dejaria de ser el que alguien calculo.
     */
    private static final int MAX_MRR_SCALE = 2;

    private final Long id;
    private final Long companyId;
    private final ActivityPeriodKey periodKey;
    private final CommercialState commercialState;
    private final int activeDays;
    private final int activeUsers;
    private final int recordsCreated;

    /**
     * El MRR <b>ya normalizado a mensual</b>. Guardarlo evita recalcular el pasado
     * y evita que dos procesos lo calculen distinto —que es la forma de que el
     * mismo mes valga dos cosas segun quien pregunte—.
     */
    private final BigDecimal mrrSnapshot;

    private final LocalDateTime createdDate;
    private final Long version;

    public CompanyActivityMonth(Long id, Long companyId, ActivityPeriodKey periodKey,
            CommercialState commercialState, int activeDays, int activeUsers, int recordsCreated,
            BigDecimal mrrSnapshot, LocalDateTime createdDate, Long version) {
        validate(companyId, periodKey, commercialState, activeDays, activeUsers, recordsCreated,
                mrrSnapshot, createdDate);
        this.id = id;
        this.companyId = companyId;
        this.periodKey = periodKey;
        this.commercialState = commercialState;
        this.activeDays = activeDays;
        this.activeUsers = activeUsers;
        this.recordsCreated = recordsCreated;
        this.mrrSnapshot = mrrSnapshot;
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * La fila del mes, recien nacida. Sin version: la asigna Hibernate al insertar.
     *
     * <p>
     * <strong>El alta no comprueba que el par empresa-mes este libre.</strong> Lo
     * cuida {@code uq_cam_month} en la base, que es lo unico que serializa dos
     * peticiones concurrentes; un {@code exists} previo lo pasarian las dos. El
     * duplicado llega como violacion de integridad y se traduce a
     * {@link CompanyActivityMonthAlreadyExistsException}.
     */
    public static CompanyActivityMonth record(Long companyId, ActivityPeriodKey periodKey,
            CommercialState commercialState, int activeDays, int activeUsers, int recordsCreated,
            BigDecimal mrrSnapshot, LocalDateTime createdDate) {
        return new CompanyActivityMonth(null, companyId, periodKey, commercialState, activeDays,
                activeUsers, recordsCreated, mrrSnapshot, createdDate, null);
    }

    /**
     * Recalcula el mes: los cinco numeros nuevos sobre la misma fila.
     *
     * <p>
     * Devuelve una instancia nueva —la clase no tiene mutadores—
     * <strong>conservando id, empresa, periodo, fecha de creacion y
     * version</strong>. Que la version viaje es lo que hace que el {@code save}
     * posterior siga siendo un ciclo leer-modificar-guardar con bloqueo optimista y
     * no un insert: si se perdiera, Hibernate tomaria la entidad por transitoria y
     * escribiria una fila nueva, que ademas chocaria contra {@code uq_cam_month}.
     *
     * <p>
     * <strong>La empresa y el periodo no se pueden cambiar aqui, y es
     * deliberado.</strong> Mover una fila de actividad de una clinica a otra o de
     * un mes a otro no es un recalculo: es reescribir la historia. Si el par
     * estuviera mal, la fila se borra y se vuelve a dar de alta —y borrarla es una
     * decision consciente, no un efecto secundario de un {@code update}—.
     */
    public CompanyActivityMonth recalculate(CommercialState commercialState, int activeDays,
            int activeUsers, int recordsCreated, BigDecimal mrrSnapshot) {
        return new CompanyActivityMonth(id, companyId, periodKey, commercialState, activeDays,
                activeUsers, recordsCreated, mrrSnapshot, createdDate, version);
    }

    /**
     * Si la clinica estuvo dormida ese mes segun un umbral de dias.
     *
     * <p>
     * El umbral no es una constante del dominio porque no hay un numero
     * universalmente correcto: «dormido» son tres dias para quien mira retencion y
     * cero para quien mira bajas. Quien pregunta trae su umbral, y el mismo umbral
     * viaja al barrido que usa {@code ix_cam_dormant}.
     */
    public boolean isDormant(int activeDaysThreshold) {
        return activeDays <= activeDaysThreshold;
    }

    /** Si el mes se facturo. Es {@code PAID} y nada mas: gratis no es pagar. */
    public boolean isPaid() {
        return commercialState == CommercialState.PAID;
    }

    private static void validate(Long companyId, ActivityPeriodKey periodKey,
            CommercialState commercialState, int activeDays, int activeUsers, int recordsCreated,
            BigDecimal mrrSnapshot, LocalDateTime createdDate) {
        if (companyId == null) {
            throw new IllegalArgumentException("companyId is required");
        }
        if (periodKey == null) {
            throw new IllegalArgumentException("periodKey is required");
        }
        if (commercialState == null) {
            throw new IllegalArgumentException("commercialState is required");
        }
        validateActiveDays(periodKey, activeDays);
        validateCounter("activeUsers", activeUsers);
        validateCounter("recordsCreated", recordsCreated);
        validateMrr(mrrSnapshot);
        if (createdDate == null) {
            throw new IllegalArgumentException("createdDate is required");
        }
    }

    /**
     * Espejo de {@code chk_cam_active_days} <b>mas</b> el techo real del mes, que
     * la constraint no mira.
     *
     * <p>
     * El motor acepta 31 dias activos en febrero porque su {@code CHECK} no puede
     * consultar el calendario sin funciones que las restricciones no admiten. Aqui
     * si se puede, y no comprobarlo dejaria pasar un numero imposible que despues
     * aparece en un informe como si fuera medicion.
     */
    private static void validateActiveDays(ActivityPeriodKey periodKey, int activeDays) {
        if (activeDays < 0 || activeDays > MAX_ACTIVE_DAYS) {
            throw new IllegalArgumentException(
                    "activeDays must be between 0 and " + MAX_ACTIVE_DAYS + ": " + activeDays);
        }
        int lengthOfMonth = periodKey.lengthOfMonth();
        if (activeDays > lengthOfMonth) {
            throw new IllegalArgumentException("activeDays cannot exceed the " + lengthOfMonth
                    + " days of " + periodKey.value() + ": " + activeDays);
        }
    }

    /** Espejo de {@code chk_cam_active_users} y {@code chk_cam_records}. */
    private static void validateCounter(String name, int value) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative: " + value);
        }
    }

    /**
     * Espejo de {@code chk_cam_mrr} mas la escala, que la constraint no puede
     * expresar: {@code DECIMAL(19,2)} no rechaza un tercer decimal, lo
     * <em>redondea</em>, y el MRR guardado deja de ser el escrito.
     *
     * <p>
     * Cero es valido —un mes {@code FREE}, {@code TRIAL} o {@code CHURNED} tiene
     * MRR cero— y por eso la comprobacion es {@code < 0} y no {@code <= 0}.
     */
    private static void validateMrr(BigDecimal mrrSnapshot) {
        if (mrrSnapshot == null) {
            throw new IllegalArgumentException("mrrSnapshot is required");
        }
        if (mrrSnapshot.signum() < 0) {
            throw new IllegalArgumentException("mrrSnapshot must not be negative: " + mrrSnapshot);
        }
        if (mrrSnapshot.scale() > MAX_MRR_SCALE) {
            throw new IllegalArgumentException("mrrSnapshot must have 2 decimals or fewer");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public ActivityPeriodKey getPeriodKey() {
        return periodKey;
    }

    public CommercialState getCommercialState() {
        return commercialState;
    }

    public int getActiveDays() {
        return activeDays;
    }

    public int getActiveUsers() {
        return activeUsers;
    }

    public int getRecordsCreated() {
        return recordsCreated;
    }

    public BigDecimal getMrrSnapshot() {
        return mrrSnapshot;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
