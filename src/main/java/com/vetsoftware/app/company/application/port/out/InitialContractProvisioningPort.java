package com.vetsoftware.app.company.application.port.out;

/**
 * Materializa el contrato y los accesos mínimos de una empresa recién creada.
 */
public interface InitialContractProvisioningPort {

    void provisionForCompany(Long companyId);
}
