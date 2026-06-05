package com.vetsoftware.app.tax.application.port.out;

import com.vetsoftware.app.tax.domain.Tax;
import java.util.List;
import java.util.Optional;

public interface TaxRepository {
    Tax save(Tax tax);
    Optional<Tax> findById(Long id);
    List<Tax> findAllByCompanyId(Long companyId);
    void delete(Long id);
    int reactivate(Long id);
}
