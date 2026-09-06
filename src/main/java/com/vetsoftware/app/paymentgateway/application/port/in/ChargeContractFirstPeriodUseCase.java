package com.vetsoftware.app.paymentgateway.application.port.in;

import com.vetsoftware.app.paymentgateway.application.command.ChargeContractFirstPeriodCommand;
import com.vetsoftware.app.paymentgateway.application.dto.FirstPeriodChargeDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Cobra el primer periodo de un contrato recién firmado.
 *
 * <p>
 * <strong>Cerrado a {@code hasRole('SYSTEM')} a secas.</strong> Lo dispara
 * {@code subscription} desde {@code afterCommit}, ya bajo
 * {@code SystemAuthRunner}: un cliente no se cobra a sí mismo.
 */
public interface ChargeContractFirstPeriodUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    FirstPeriodChargeDto execute(ChargeContractFirstPeriodCommand command);
}
