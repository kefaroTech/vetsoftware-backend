package com.vetsoftware.app.paymentgateway.application.port.out;

import com.vetsoftware.app.paymentgateway.domain.CurrentContractRef;
import java.util.Optional;

/**
 * El contrato vigente de una empresa, que es de otra feature
 * ({@code subscription}).
 */
public interface CurrentContractQueryPort {
    Optional<CurrentContractRef> findCurrent(Long companyId);
}
