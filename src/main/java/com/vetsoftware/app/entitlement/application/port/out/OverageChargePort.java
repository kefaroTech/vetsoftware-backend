package com.vetsoftware.app.entitlement.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Devenga el cargo por excedente contra el contrato de la clinica.
 *
 * <p>
 * <strong>No es el gemelo de {@link LimitDenialPort}, y la diferencia es toda
 * la intencion.</strong> Aquel escribe el portazo en transaccion propia
 * ({@code REQUIRES_NEW}) precisamente <em>para sobrevivir</em> a la vuelta
 * atras de la operacion que lo provoco: el hecho de que se nego es cierto
 * aunque la peticion se revierta. Este va en la <b>misma transaccion</b> que el
 * consumo, porque si el cargo no se puede escribir, el consumo por encima del
 * techo <b>tampoco debe quedar</b> — de lo contrario la clinica se pasa del
 * cupo gratis y no hay ninguna fila que lo reclame. Consumir de mas y cobrarlo
 * son un solo hecho o no son ninguno.
 *
 * <p>
 * Por lo mismo, el adaptador <strong>no se traga las excepciones</strong>: una
 * que llegue aqui tiene que tumbar la operacion entera.
 *
 * <p>
 * Los tipos son primitivos y de {@code java.time} a proposito: el dominio de
 * facturacion —{@code ServicePeriod}, {@code ChargeType}, {@code TaxTreatment}—
 * pertenece a otra rodaja y no puede cruzar hasta el adaptador.
 *
 * @param overageUnits
 *            unidades por encima del techo. Siempre mayor que cero
 * @param unitAmount
 *            precio por unidad copiado del contrato
 * @param servicePeriodStart
 *            primer dia del periodo consumido
 * @param servicePeriodEnd
 *            ultimo dia del periodo consumido, inclusive
 */
public interface OverageChargePort {

    void chargeOverage(Long companyId, Long subscriptionId, Long subscriptionItemId,
            String dimensionCode, int overageUnits, BigDecimal unitAmount,
            LocalDate servicePeriodStart, LocalDate servicePeriodEnd);
}
