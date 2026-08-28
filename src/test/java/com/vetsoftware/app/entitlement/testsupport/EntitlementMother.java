package com.vetsoftware.app.entitlement.testsupport;

import com.vetsoftware.app.entitlement.domain.CapacityGrantLine;
import com.vetsoftware.app.entitlement.domain.LimitDimensionRef;
import com.vetsoftware.app.entitlement.domain.MeasureKind;
import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import com.vetsoftware.app.entitlement.domain.CompanyEntitlement;
import com.vetsoftware.app.entitlement.domain.ContractSnapshot;
import com.vetsoftware.app.entitlement.domain.ContractStatus;
import com.vetsoftware.app.entitlement.domain.ModuleGrantLine;
import com.vetsoftware.app.entitlement.domain.PeriodKey;
import com.vetsoftware.app.entitlement.domain.ResetPeriod;
import com.vetsoftware.app.entitlement.domain.SubModuleRef;
import com.vetsoftware.app.entitlement.domain.SubscriptionRef;
import com.vetsoftware.app.entitlement.domain.LineChargeMode;
import com.vetsoftware.app.entitlement.domain.TrialOutcomePolicy;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Datos de prueba del slice. El reloj es fijo a proposito: la caducidad de la
 * prueba y la idempotencia solo se pueden afirmar contra un instante que no se
 * mueva entre dos lineas del test.
 */
public final class EntitlementMother {

    public static final Long COMPANY_ID = 10L;
    public static final Long SUBSCRIPTION_ID = 500L;
    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 15, 9, 0);
    public static final LocalDate HOY = AHORA.toLocalDate();

    /**
     * El dia en que esta empresa firmo ({@code subscriptions.start_date}). Es la
     * fecha contra la que D-74 decide si un eje le aplica o no.
     */
    public static final LocalDate FIRMA = LocalDate.of(2026, 1, 10);

    /**
     * Fecha de nacimiento de los ejes que ya existian cuando se firmo. Anterior a
     * {@link #FIRMA} a proposito: sobre estos, no tener fila significa techo cero,
     * que es la regla de siempre.
     */
    private static final LocalDate ANTES_DE_LA_FIRMA = FIRMA.minusMonths(4);

    /**
     * Fecha de nacimiento del eje que llego <strong>despues</strong> de la firma.
     * Es el caso de D-74: sobre este eje, esta empresa no tiene techo.
     */
    public static final LocalDate DESPUES_DE_LA_FIRMA = FIRMA.plusMonths(3);

    /**
     * Los ejes tal como los siembra el changeset 313. Los ids son los del andamio y
     * no los reales: lo que importa aqui es que el contador se identifique por una
     * referencia al catalogo y no por un enumerado cerrado.
     */
    public static final LimitDimensionRef USUARIOS = new LimitDimensionRef(41L, "USER",
            MeasureKind.STOCK, ANTES_DE_LA_FIRMA);
    public static final LimitDimensionRef SEDES = new LimitDimensionRef(42L, "BRANCH",
            MeasureKind.STOCK, ANTES_DE_LA_FIRMA);

    /**
     * Un eje que la lista cerrada de cuatro unidades no podia nombrar. Existe en el
     * andamio para que las pruebas puedan afirmar sobre un eje del que nunca hubo
     * constante en Java: si vender uno nuevo siguiera exigiendo un despliegue, esta
     * linea no compilaria.
     */
    public static final LimitDimensionRef MASCOTAS = new LimitDimensionRef(43L, "ANIMAL",
            MeasureKind.CUMULATIVE, ANTES_DE_LA_FIRMA);

    /** El unico de flujo del andamio: es el que exige clave de periodo real. */
    public static final LimitDimensionRef CITAS = new LimitDimensionRef(44L, "APPOINTMENT",
            MeasureKind.FLOW, ANTES_DE_LA_FIRMA);

    /**
     * El mismo eje de citas, pero nacido <strong>despues</strong> de que esta
     * empresa firmara: el caso violador de D-74, «añadir el eje APPOINTMENT en
     * abril no deja bloqueadas las agendas de los contratos firmados en enero».
     */
    public static final LimitDimensionRef CITAS_POSTERIORES = new LimitDimensionRef(44L,
            "APPOINTMENT", MeasureKind.FLOW, DESPUES_DE_LA_FIRMA);

    /** Un eje de existencias nacido despues de la firma, para el mismo caso. */
    public static final LimitDimensionRef TERMINALES_POSTERIORES = new LimitDimensionRef(45L,
            "TERMINAL", MeasureKind.STOCK, DESPUES_DE_LA_FIRMA);

    private EntitlementMother() {
    }

    public static Clock relojFijo() {
        return Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    /** Sabe funcionar en solo lectura: consultar e imprimir la historia. */
    public static SubModuleRef historiaClinica() {
        return new SubModuleRef(1L, "CLINICAL_HISTORY", "Historia clinica");
    }

    /** No sabe funcionar en solo lectura: emitir factura es escribir o nada. */
    public static SubModuleRef facturacion() {
        return new SubModuleRef(2L, "BILLING", "Facturacion electronica");
    }

    public static SubscriptionRef contratoEn(ContractStatus status) {
        LocalDate finPrueba = status == ContractStatus.TRIALING ? HOY.plusDays(5) : null;
        return new SubscriptionRef(SUBSCRIPTION_ID, status, finPrueba, FIRMA);
    }

    public static SubscriptionRef enPruebaHasta(LocalDate finPrueba) {
        return new SubscriptionRef(SUBSCRIPTION_ID, ContractStatus.TRIALING, finPrueba, FIRMA);
    }

    public static ModuleGrantLine lineaVigente(Long itemId, SubModuleRef subModule,
            boolean readOnlyCapable) {
        return new ModuleGrantLine(itemId, subModule, readOnlyCapable, HOY.minusMonths(2), null,
                false);
    }

    public static ModuleGrantLine lineaVigenteDeNucleo(Long itemId, SubModuleRef subModule) {
        return new ModuleGrantLine(itemId, subModule, true, HOY.minusMonths(2), null, true);
    }

    public static ModuleGrantLine lineaTerminada(Long itemId, SubModuleRef subModule,
            boolean readOnlyCapable, LocalDate fin) {
        return new ModuleGrantLine(itemId, subModule, readOnlyCapable, HOY.minusMonths(6), fin,
                false);
    }

    /**
     * Una linea <strong>en prueba</strong>, con su ultimo dia y su desenlace ya
     * congelado. Los dos datos van en la linea y no en el contrato a proposito:
     * cada linea vence por su cuenta (R-TRIAL-15).
     */
    public static ModuleGrantLine lineaEnPrueba(Long itemId, SubModuleRef subModule,
            boolean readOnlyCapable, LocalDate ultimoDiaDePrueba, TrialOutcomePolicy desenlace) {
        return new ModuleGrantLine(itemId, subModule, readOnlyCapable, HOY.minusMonths(2), null,
                false, LineChargeMode.TRIAL, ultimoDiaDePrueba, desenlace, false);
    }

    /**
     * Una linea de un submodulo <strong>inmune a la degradacion</strong>
     * (R-ENT-05): no baja ni por mora, ni por cupo, ni por baja.
     */
    public static ModuleGrantLine lineaInmuneALaDegradacion(Long itemId, SubModuleRef subModule) {
        return new ModuleGrantLine(itemId, subModule, false, HOY.minusMonths(2), null, false,
                LineChargeMode.PAID, null, null, true);
    }

    /**
     * Una linea de capacidad vigente de un eje que <strong>no</strong> es de flujo:
     * sin granularidad, porque un cupo que no se mide por periodo no reinicia nada
     * y el dominio lo rechaza si se la pasas.
     */
    public static CapacityGrantLine capacidadVigente(Long itemId, LimitDimensionRef eje,
            int incluidas, int compradas) {
        return new CapacityGrantLine(itemId, eje, compradas, incluidas, null, HOY.minusMonths(2),
                null);
    }

    /**
     * Una linea de capacidad vigente de un eje <strong>de flujo</strong>, con la
     * granularidad que la venta congelo. Es la que hace nacer la primera fila de la
     * serie del contador; las de los periodos siguientes heredan de ella.
     */
    public static CapacityGrantLine capacidadDeFlujoVigente(Long itemId, LimitDimensionRef eje,
            int incluidas, int compradas, ResetPeriod periodo) {
        return new CapacityGrantLine(itemId, eje, compradas, incluidas, periodo, HOY.minusMonths(2),
                null);
    }

    public static ContractSnapshot contrato(SubscriptionRef subscription,
            List<ModuleGrantLine> moduleLines, List<CapacityGrantLine> capacityLines) {
        return new ContractSnapshot(subscription, moduleLines, capacityLines);
    }

    /**
     * El caso normal: contrato activo con un modulo que sabe hacer solo lectura.
     */
    public static ContractSnapshot contratoActivoConHistoria() {
        return contrato(contratoEn(ContractStatus.ACTIVE),
                List.of(lineaVigente(900L, historiaClinica(), true)), List.of());
    }

    /**
     * Un contador que ya existia. Nace con el sello del consumo <strong>a
     * nulo</strong> a proposito: mientras nadie haya recontado las filas reales,
     * esa es la respuesta honesta, y varias pruebas afirman justamente que el
     * recalculo no lo inventa.
     */
    public static CompanyCapacity contadorExistente(Long id, LimitDimensionRef eje, int techo,
            int usado) {
        return new CompanyCapacity(id, COMPANY_ID, eje,
                PeriodKey.forMeasure(eje.measureKind(), null), techo, usado, SUBSCRIPTION_ID,
                AHORA.minusDays(30), null, AHORA.minusDays(90));
    }

    /** El mismo contador, pero con el consumo ya recontado en un instante dado. */
    public static CompanyCapacity contadorRecontado(Long id, LimitDimensionRef eje, int techo,
            int usado, LocalDateTime selloDelConsumo) {
        return new CompanyCapacity(id, COMPANY_ID, eje,
                PeriodKey.forMeasure(eje.measureKind(), null), techo, usado, SUBSCRIPTION_ID,
                AHORA.minusDays(30), selloDelConsumo, AHORA.minusDays(90));
    }

    /**
     * Un contador de un eje de flujo, anclado al periodo que se le diga. Es el
     * ancestro del que hereda su techo la fila del periodo siguiente (R-LIMIT-04).
     */
    public static CompanyCapacity contadorDeFlujo(Long id, LimitDimensionRef eje, String periodo,
            int techo, int usado) {
        return new CompanyCapacity(id, COMPANY_ID, eje, PeriodKey.of(periodo), techo, usado,
                SUBSCRIPTION_ID, AHORA.minusDays(30), null, AHORA.minusDays(30));
    }

    public static CompanyEntitlement permisoExistente(SubModuleRef subModule,
            com.vetsoftware.app.entitlement.domain.AccessLevel level) {
        return new CompanyEntitlement(77L, COMPANY_ID, subModule, level,
                com.vetsoftware.app.entitlement.domain.EntitlementSource.SUBSCRIPTION,
                SUBSCRIPTION_ID, 900L, AHORA.minusDays(60), null, AHORA.minusDays(1),
                AHORA.minusDays(60));
    }
}
