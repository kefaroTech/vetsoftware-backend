package com.vetsoftware.app.companytrialwindow.infrastructure.persistence;

import com.vetsoftware.app.companytrialwindow.application.port.out.CompanyTrialWindowRepository;
import com.vetsoftware.app.companytrialwindow.domain.CompanyTrialWindow;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adaptador de salida del reloj de la empresa. */
@Repository
public class JpaCompanyTrialWindowRepository implements CompanyTrialWindowRepository {

    private final CompanyTrialWindowJpaRepository jpaRepository;
    private final CompanyTrialWindowJpaMapper mapper;

    public JpaCompanyTrialWindowRepository(CompanyTrialWindowJpaRepository jpaRepository,
            CompanyTrialWindowJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CompanyTrialWindow save(CompanyTrialWindow window) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(window)));
    }

    @Override
    public Optional<CompanyTrialWindow> findOpenByCompanyId(Long companyId) {
        return jpaRepository.findByCompanyIdAndClosedAtIsNull(companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<CompanyTrialWindow> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public boolean existsOpenByCompanyId(Long companyId) {
        return jpaRepository.existsByCompanyIdAndClosedAtIsNull(companyId);
    }
}
