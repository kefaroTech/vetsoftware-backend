package com.vetsoftware.app.auth.domain;

/**
 * La empresa tiene el submódulo en {@code READ_ONLY} y el request pedía
 * escribir en él. La lanza {@code Authz#requireModuleWritable}; lleva
 * {@code type} propio para que el front la distinga de un 403 por falta de
 * permiso.
 */
public class SubModuleReadOnlyException extends RuntimeException {

    private final Long companyId;
    private final String subModuleCode;

    public SubModuleReadOnlyException(Long companyId, String subModuleCode) {
        super("Company " + companyId + " has sub module " + subModuleCode
                + " in READ_ONLY: writes are blocked, reads still work");
        this.companyId = companyId;
        this.subModuleCode = subModuleCode;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getSubModuleCode() {
        return subModuleCode;
    }
}
