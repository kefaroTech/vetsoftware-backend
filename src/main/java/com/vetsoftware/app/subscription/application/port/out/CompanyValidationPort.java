package com.vetsoftware.app.subscription.application.port.out;

/**
 * Solo valida existencia: este slice no necesita ni un dato de la empresa, solo
 * el discriminador de tenant. Por eso el dominio guarda {@code Long companyId}
 * y no un companion VO.
 */
public interface CompanyValidationPort {
    void validateExists(Long companyId);
}
