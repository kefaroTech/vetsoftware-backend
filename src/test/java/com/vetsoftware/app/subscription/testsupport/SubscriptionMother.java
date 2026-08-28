package com.vetsoftware.app.subscription.testsupport;

import com.vetsoftware.app.subscription.application.dto.SubscriptionAmendmentDto;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemDto;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemOverlapDto;
import com.vetsoftware.app.subscription.application.dto.SubscriptionStatusChangeDto;
import com.vetsoftware.app.subscription.domain.AmendmentType;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.EffectivePeriod;
import com.vetsoftware.app.subscription.domain.ItemOrigin;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionAmendment;
import com.vetsoftware.app.subscription.domain.SubscriptionItem;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChange;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fixtures del slice {@code subscription}. Un metodo por variante y valores
 * validos por defecto, para que cada test solo tenga que nombrar lo que de
 * verdad esta probando.
 *
 * <p>
 * <b>Todas las fechas son literales.</b> Ni un {@code now()}: este slice es
 * fechas de arriba abajo y un fixture que mire el reloj convierte cualquier
 * asercion de vigencia en un test que se cae solo al cruzar medianoche.
 */
public final class SubscriptionMother {

    public static final Long EMPRESA = 42L;
    public static final Long CONTRATO = 7L;
    public static final Long ARTICULO = 100L;
    public static final Long EMPLEADO = 4L;
    public static final Long USUARIO_DE_PLATAFORMA = 6L;

    public static final LocalDate ENERO_1 = LocalDate.of(2026, 1, 1);
    public static final LocalDate ENERO_31 = LocalDate.of(2026, 1, 31);
    public static final LocalDate MAYO_1 = LocalDate.of(2026, 5, 1);
    public static final LocalDate JUNIO_30 = LocalDate.of(2026, 6, 30);
    public static final LocalDate DICIEMBRE_31 = LocalDate.of(2026, 12, 31);
    public static final LocalDateTime MOMENTO = LocalDateTime.of(2026, 1, 15, 10, 15, 30);

    public static final BigDecimal PRECIO = new BigDecimal("179000.00");
    public static final BigDecimal IVA = new BigDecimal("19.00");

    private SubscriptionMother() {
    }

    /** Contrato ACTIVE, mensual, del primer periodo de 2026. */
    public static Subscription contratoVigente() {
        return contratoEn(SubscriptionStatus.ACTIVE);
    }

    public static Subscription contratoEn(SubscriptionStatus status) {
        return Subscription.create("SUS-2026-00184", EMPRESA, null, 3L, BillingCycle.MONTHLY,
                status, ENERO_1,
                status == SubscriptionStatus.TRIALING ? LocalDate.of(2026, 1, 15) : null, ENERO_1,
                ENERO_31, ENERO_31, null, 5, true);
    }

    /** Linea abierta de capacidad: 5 usuarios con 2 incluidos. */
    public static SubscriptionItem lineaAbierta() {
        return lineaDesde(ENERO_1);
    }

    public static SubscriptionItem lineaDesde(LocalDate desde) {
        return lineaEntre(desde, null);
    }

    public static SubscriptionItem lineaEntre(LocalDate desde, LocalDate hasta) {
        return SubscriptionItem.open(EMPRESA, CONTRATO, ARTICULO, "EXTRA_USER", "Usuario adicional",
                SubscriptionItemType.CAPACITY, "USER", 2, TaxTreatment.TAXED, 5, PRECIO, IVA,
                new EffectivePeriod(desde, hasta), ItemOrigin.ADDON, 11L);
    }

    /** Modulo sin unidad de capacidad, que es lo que exige el dominio. */
    public static SubscriptionItem moduloAbierto() {
        return SubscriptionItem.open(EMPRESA, CONTRATO, ARTICULO, "CORE", "Nucleo",
                SubscriptionItemType.MODULE, null, 0, TaxTreatment.TAXED, 1, PRECIO, IVA,
                EffectivePeriod.openFrom(ENERO_1), ItemOrigin.INITIAL, null);
    }

    public static SubscriptionAmendment otrosiFirmadoPorEmpleado() {
        return otrosi(EMPLEADO, null);
    }

    public static SubscriptionAmendment otrosiFirmadoPorLaPlataforma() {
        return otrosi(null, USUARIO_DE_PLATAFORMA);
    }

    public static SubscriptionAmendment otrosi(Long empleadoId, Long systemUserId) {
        return SubscriptionAmendment.issue(EMPRESA, CONTRATO, "AMD-2026-00001",
                AmendmentType.ADD_ITEM, MAYO_1, "Ampliacion de usuarios", empleadoId, systemUserId,
                BigDecimal.ZERO, PRECIO, null, "req-1");
    }

    public static SubscriptionStatusChange transicion() {
        return SubscriptionStatusChange.record(EMPRESA, CONTRATO, SubscriptionStatus.ACTIVE,
                SubscriptionStatus.PAST_DUE, "Cuota vencida", "cobranza", MOMENTO);
    }

    /** DTO de contrato, listo para devolver desde un puerto doblado. */
    public static SubscriptionDto dto() {
        return SubscriptionDto.from(contratoVigente());
    }

    public static SubscriptionItemDto itemDto() {
        return SubscriptionItemDto.from(lineaAbierta());
    }

    public static SubscriptionAmendmentDto amendmentDto() {
        return SubscriptionAmendmentDto.from(otrosiFirmadoPorEmpleado());
    }

    public static SubscriptionStatusChangeDto statusChangeDto() {
        return SubscriptionStatusChangeDto.from(transicion());
    }

    /**
     * Un par de tramos del mismo articulo que se pisan: lo que devuelve la
     * vigilancia R7 cuando el invariante ya se rompio.
     */
    public static SubscriptionItemOverlapDto solapeDto() {
        return new SubscriptionItemOverlapDto(EMPRESA, CONTRATO, ARTICULO, "EXTRA_USER", 1L,
                ENERO_1, JUNIO_30, 2L, MAYO_1, DICIEMBRE_31);
    }
}
