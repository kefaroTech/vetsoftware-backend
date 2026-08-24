package com.vetsoftware.app.entitlement.testsupport;

import com.vetsoftware.app.entitlement.domain.CapacityGrantLine;
import com.vetsoftware.app.entitlement.domain.CapacityUnit;
import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import com.vetsoftware.app.entitlement.domain.CompanyEntitlement;
import com.vetsoftware.app.entitlement.domain.ContractSnapshot;
import com.vetsoftware.app.entitlement.domain.ContractStatus;
import com.vetsoftware.app.entitlement.domain.ModuleGrantLine;
import com.vetsoftware.app.entitlement.domain.SubModuleRef;
import com.vetsoftware.app.entitlement.domain.SubscriptionRef;
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
        return new SubscriptionRef(SUBSCRIPTION_ID, status, finPrueba);
    }

    public static SubscriptionRef enPruebaHasta(LocalDate finPrueba) {
        return new SubscriptionRef(SUBSCRIPTION_ID, ContractStatus.TRIALING, finPrueba);
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

    public static CapacityGrantLine capacidadVigente(Long itemId, CapacityUnit unit, int incluidas,
            int compradas) {
        return new CapacityGrantLine(itemId, unit, compradas, incluidas, HOY.minusMonths(2), null);
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

    public static CompanyCapacity contadorExistente(Long id, CapacityUnit unit, int techo,
            int usado) {
        return new CompanyCapacity(id, COMPANY_ID, unit, techo, usado, SUBSCRIPTION_ID,
                AHORA.minusDays(30), AHORA.minusDays(90));
    }

    public static CompanyEntitlement permisoExistente(SubModuleRef subModule,
            com.vetsoftware.app.entitlement.domain.AccessLevel level) {
        return new CompanyEntitlement(77L, COMPANY_ID, subModule, level,
                com.vetsoftware.app.entitlement.domain.EntitlementSource.SUBSCRIPTION,
                SUBSCRIPTION_ID, 900L, AHORA.minusDays(60), null, AHORA.minusDays(1),
                AHORA.minusDays(60));
    }
}
