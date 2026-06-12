package com.vetsoftware.app.companytaxprofile.application.port.out;

import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfile;
import java.util.Optional;

public interface CompanyTaxProfileRepository {
    CompanyTaxProfile save(CompanyTaxProfile profile);
    Optional<CompanyTaxProfile> findByCompanyId(Long companyId);
    boolean existsByCompanyId(Long companyId);
    void delete(Long companyId);
    int reactivate(Long companyId);
}
