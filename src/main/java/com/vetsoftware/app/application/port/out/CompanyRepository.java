package com.vetsoftware.app.application.port.out;

import com.vetsoftware.app.domain.Company;
import com.vetsoftware.app.domain.CompanyId;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository {
    void save(Company company);
    Optional<Company> findById(CompanyId id);
    List<Company> findAll();
    void delete(CompanyId id);
}
