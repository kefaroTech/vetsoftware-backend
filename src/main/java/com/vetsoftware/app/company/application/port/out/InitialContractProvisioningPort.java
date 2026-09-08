package com.vetsoftware.app.company.application.port.out;

/**
 * Materializa el contrato y los accesos mínimos de una empresa recién creada:
 * la misma ventana de prueba, las mismas concesiones y las mismas líneas
 * {@code TRIAL} que firma el alta pública.
 */
public interface InitialContractProvisioningPort {

    /**
     * @param companyName
     *            solo para que, si falta catálogo, el error de configuración sea
     *            legible por un humano
     */
    void provisionForCompany(Long companyId, String companyName);
}
