package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.company.application.command.UpdateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;

public interface UpdateCompanyUseCase {
    CompanyDto execute(UpdateCompanyCommand command);
}
