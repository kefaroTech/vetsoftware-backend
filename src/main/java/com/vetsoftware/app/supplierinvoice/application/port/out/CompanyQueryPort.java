package com.vetsoftware.app.supplierinvoice.application.port.out;

import com.vetsoftware.app.supplierinvoice.domain.CompanyRef;
import java.util.Optional;

/** Carga el companion VO de la empresa (feature {@code company}). */
public interface CompanyQueryPort {
    Optional<CompanyRef> findById(Long companyId);
}
