package com.vetsoftware.app.application.port.in;

import com.vetsoftware.app.application.dto.CompanyDto;
import java.util.List;

public interface ListCompaniesUseCase {
    List<CompanyDto> listAll();
}
