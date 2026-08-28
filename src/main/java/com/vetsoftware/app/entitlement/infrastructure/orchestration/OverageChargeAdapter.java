package com.vetsoftware.app.entitlement.infrastructure.orchestration;

import com.vetsoftware.app.entitlement.application.port.out.OverageChargePort;
import com.vetsoftware.app.subscriptionbilling.application.command.AccrueOverageChargeCommand;
import com.vetsoftware.app.subscriptionbilling.application.port.in.AccrueOverageChargeUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Devenga el cargo por excedente contra el contrato de la clinica.
 *
 * <p>
 * <strong>Este adaptador es la mitad que faltaba del modelo.</strong>
 * {@code subscription_item_limits.enforcement} admite {@code OVERAGE} desde el
 * changeset 304 y {@code SubscriptionItemLimit} lo valida desde entonces —exige
 * precio por unidad positivo y prohibe el eje acumulativo—, pero no habia clase
 * de cargo a la que mandarlo. Sin destino, el contador de cupo hacia lo unico
 * que podia: negar. Es decir, <b>se bloqueaba a un cliente que estaba dispuesto
 * a pagar el excedente y que el propio modelo dice que debe poder pasarse</b>.
 *
 * <p>
 * <strong>No es el gemelo de {@link LimitDenialAdapter}, y hay que leer la
 * diferencia antes de tocar nada.</strong> Aquel se traga las excepciones a
 * proposito —si la bitacora falla, al usuario le tiene que llegar «se te acabo
 * el cupo» y no un 500, porque la negacion ya estaba decidida— y escribe en
 * transaccion propia para sobrevivir a la vuelta atras. Aqui es al reves en las
 * dos cosas:
 *
 * <ul>
 * <li><b>Misma transaccion, sin {@code REQUIRES_NEW}.</b>
 * {@link AccrueOverageChargeUseCase} se une a la transaccion del contador, asi
 * que el consumo por encima del techo y el cargo que lo cobra <b>viven o mueren
 * juntos</b>. Escribirlo aparte dejaria pasar el consumo con el cargo
 * revertido: la clinica se pasa del cupo gratis y no queda ni una fila que lo
 * reclame.
 * <li><b>Nada de {@code catch}.</b> Una excepcion aqui tiene que tumbar la
 * operacion entera. Tragarsela seria exactamente el defecto anterior con otra
 * cara.
 * </ul>
 *
 * <p>
 * <strong>Delega en {@link AccrueOverageChargeUseCase} y NO en el puerto de
 * alta general, y esa eleccion es la que hace que el arreglo funcione de
 * verdad.</strong> {@code CreateSubscriptionChargeUseCase} esta cerrado a
 * {@code hasRole('SYSTEM')} a secas, pero <b>el consumo que dispara el
 * excedente lo hace un empleado del tenant</b> —el gate de
 * {@code AdjustCompanyCapacityUsageUseCase} es {@code @authz.isMyCompany}—, asi
 * que encaminarlo por alli dejaria al cliente igual de bloqueado que antes,
 * solo que con un 403 en vez de un 409. El puerto dedicado lleva el gate de
 * {@code RecordLimitEventUseCase}
 * —{@code hasRole('SYSTEM') or @authz.isMyCompany(#command.companyId)}— porque
 * es lo mismo que aquel: un efecto del sistema, no una capacidad concedible.
 */
@Component
public class OverageChargeAdapter implements OverageChargePort {

    private final AccrueOverageChargeUseCase accrueOverageCharge;

    public OverageChargeAdapter(AccrueOverageChargeUseCase accrueOverageCharge) {
        this.accrueOverageCharge = accrueOverageCharge;
    }

    @Override
    public void chargeOverage(Long companyId, Long subscriptionId, Long subscriptionItemId,
            String dimensionCode, int overageUnits, BigDecimal unitAmount,
            LocalDate servicePeriodStart, LocalDate servicePeriodEnd) {
        accrueOverageCharge.execute(new AccrueOverageChargeCommand(companyId, subscriptionId,
                subscriptionItemId, descripcion(dimensionCode, overageUnits), servicePeriodStart,
                servicePeriodEnd, overageUnits, unitAmount));
    }

    /**
     * La descripcion nombra el eje y las unidades porque es lo que el cliente lee
     * en su cuenta de cobro: «Excedente» a secas obliga a llamar a soporte para
     * saber de que.
     */
    private static String descripcion(String dimensionCode, int overageUnits) {
        return "Excedente de " + overageUnits + " unidad" + (overageUnits == 1 ? "" : "es")
                + " sobre el cupo contratado de " + dimensionCode;
    }
}
