package com.vetsoftware.app.application.port.out;

import com.vetsoftware.app.domain.Company;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository {
    Company save(Company company);
    Optional<Company> findById(Long id);
    List<Company> findAll();
    void delete(Long id);
}
