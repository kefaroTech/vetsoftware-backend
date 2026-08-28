package com.vetsoftware.app.entitlement.application.dto;

import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import com.vetsoftware.app.entitlement.domain.LimitDimensionRef;
import com.vetsoftware.app.entitlement.domain.PeriodKey;
import java.time.LocalDateTime;

/**
 * Un contador contratado. {@code exhausted} se expone calculado porque es la
 * pregunta que hace la interfaz --ofrezco ya la ampliacion?-- y no un dato mas
 * de la fila.
 *
 * <p>
 * Los dos sellos viajan separados (R-ENT-13): {@code limitRecalculatedAt} dice
 * cuando se derivo el techo del contrato y {@code usageReconciledAt} cuando se
 * comprobo por ultima vez que el consumo cuadra con las filas reales.
 * {@code usageReconciledAt} viene {@code null} mientras nadie lo haya
 * comprobado nunca, que es la respuesta honesta y no un cero disfrazado.
 *
 * <p>
 * <strong>{@code uncapped} es el caso de D-74 y no tiene fila detras.</strong>
 * Cuando el eje nacio despues de que la empresa firmara, no hay contador --y no
 * puede haberlo, porque el contrato no lo menciona--: el consumo se permite y
 * no se cuenta contra ningun techo. Se dice con un campo propio en vez de con
 * un {@code limitQuantity} enorme porque «sin techo» y «un techo muy alto» son
 * cosas distintas, y confundirlas es como se acaba facturando un excedente que
 * nadie pacto.
 */
public record CompanyCapacityDto(Long id, Long companyId, Long limitDimensionId,
        String dimensionCode, String measureKind, String periodKey, int limitQuantity,
        int usedQuantity, boolean exhausted, boolean uncapped, Long subscriptionId,
        LocalDateTime limitRecalculatedAt, LocalDateTime usageReconciledAt) {

    public static CompanyCapacityDto from(CompanyCapacity capacity) {
        return new CompanyCapacityDto(capacity.getId(), capacity.getCompanyId(),
                capacity.getDimension().id(), capacity.getDimension().code(),
                capacity.getDimension().measureKind().name(), capacity.getPeriodKey().value(),
                capacity.getLimitQuantity(), capacity.getUsedQuantity(), capacity.isExhausted(),
                false, capacity.getSubscriptionId(), capacity.getLimitRecalculatedAt(),
                capacity.getUsageReconciledAt());
    }

    /**
     * El eje existe pero es <strong>posterior a la firma</strong> del contrato de
     * esta empresa (D-74): no hay fila, no hay techo y el consumo no se cuenta.
     *
     * <p>
     * Sin id, sin contador y sin sellos, porque no hay nada de eso: inventar un
     * cero aqui haria que la interfaz pintara «0 de 0» donde la verdad es «este
     * limite no le aplica». {@code exhausted} es {@code false} por definicion --sin
     * techo no hay techo que agotar--.
     */
    public static CompanyCapacityDto uncapped(Long companyId, LimitDimensionRef dimension,
            PeriodKey periodKey) {
        return new CompanyCapacityDto(null, companyId, dimension.id(), dimension.code(),
                dimension.measureKind().name(), periodKey.value(), 0, 0, false, true, null, null,
                null);
    }
}
