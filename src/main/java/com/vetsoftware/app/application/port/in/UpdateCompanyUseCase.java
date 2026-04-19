package com.vetsoftware.app.application.port.in;

import com.vetsoftware.app.application.command.UpdateCompanyCommand;
import com.vetsoftware.app.application.dto.CompanyDto;

public interface UpdateCompanyUseCase {
    CompanyDto execute(UpdateCompanyCommand command);
}
