package com.vetsoftware.app.application.port.in;

import com.vetsoftware.app.domain.CompanyId;

public interface DeleteCompanyUseCase {
    void execute(CompanyId id);
}
