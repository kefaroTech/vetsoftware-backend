package com.vetsoftware.app.paymentgateway.application.port.in;

import com.vetsoftware.app.paymentgateway.application.command.ChargeBillingDocumentCommand;
import com.vetsoftware.app.paymentgateway.application.dto.DocumentChargeDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Cobra un documento de cobro ya emitido contra el medio de pago Wompi por
 * defecto de la empresa: la renovación recurrente y el reintento de un rechazo
 * anterior comparten esta única operación.
 *
 * <p>
 * Cerrado a {@code hasRole('SYSTEM')} a secas, igual que
 * {@code ChargeContractFirstPeriodUseCase}: decidir cuándo se vuelve a pasar
 * una tarjeta es cobranza de plataforma, no algo que dispare el tenant.
 *
 * <p>
 * La mayoría de los desenlaces son omisiones normales, no errores: sin saldo,
 * con un pago ya en curso, con un rechazo duro reciente, con el presupuesto de
 * reintentos agotado, fuera de fecha o sin medio de pago activo. Solo
 * {@code APPROVED}, {@code PENDING} y {@code DECLINED} representan un intento
 * real contra Wompi.
 */
public interface ChargeBillingDocumentUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    DocumentChargeDto execute(ChargeBillingDocumentCommand command);
}
