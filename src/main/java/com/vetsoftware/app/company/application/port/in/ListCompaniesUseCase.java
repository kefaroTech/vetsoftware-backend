package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import java.util.List;

public interface ListCompaniesUseCase {
    List<CompanyDto> listAll();
}
