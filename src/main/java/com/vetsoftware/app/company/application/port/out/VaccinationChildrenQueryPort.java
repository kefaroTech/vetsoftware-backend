package com.vetsoftware.app.company.application.port.out;

public interface VaccinationChildrenQueryPort {
    boolean existsActiveByCompanyId(Long parentId);
}
