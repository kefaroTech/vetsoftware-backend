package com.vetsoftware.app.tax.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.tax.application.port.out.TaxRepository;
import com.vetsoftware.app.tax.domain.Tax;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTaxRepository implements TaxRepository {
    private final TaxJpaRepository jpaRepository;
    private final TaxJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaTaxRepository(TaxJpaRepository jpaRepository,
                            TaxJpaMapper mapper,
                            CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Tax save(Tax tax) {
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(tax.getCompany().id());
        TaxJpaEntity saved = jpaRepository.save(mapper.toJpa(tax, company));
        return mapper.toDomain(saved, tax.getCompany());
    }

    @Override
    public Optional<Tax> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCompanyIdAndName(Long companyId, String name) {
        return jpaRepository.existsByCompany_IdAndName(companyId, name);
    }

    @Override
    public boolean existsByCompanyIdAndNameExcludingId(Long companyId, String name, Long id) {
        return jpaRepository.existsByCompany_IdAndNameAndIdNot(companyId, name, id);
    }

    @Override
    public List<Tax> findAllByCompanyId(Long companyId) {
        return jpaRepository.findAllByCompanyId(companyId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public int reactivate(Long id) {
        return jpaRepository.reactivate(id);
    }
}
