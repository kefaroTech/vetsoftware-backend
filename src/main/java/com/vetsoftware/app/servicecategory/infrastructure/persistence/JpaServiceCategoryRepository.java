package com.vetsoftware.app.servicecategory.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.servicecategory.application.port.out.ServiceCategoryRepository;
import com.vetsoftware.app.servicecategory.domain.ServiceCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaServiceCategoryRepository implements ServiceCategoryRepository {
    private final ServiceCategoryJpaRepository jpaRepository;
    private final ServiceCategoryJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaServiceCategoryRepository(ServiceCategoryJpaRepository jpaRepository,
                                        ServiceCategoryJpaMapper mapper,
                                        CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public ServiceCategory save(ServiceCategory serviceCategory) {
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(serviceCategory.getCompany().id());
        ServiceCategoryJpaEntity saved = jpaRepository.save(mapper.toJpa(serviceCategory, company));
        return mapper.toDomain(saved, serviceCategory.getCompany());
    }

    @Override
    public Optional<ServiceCategory> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ServiceCategory> findAllByCompanyId(Long companyId) {
        return jpaRepository.findAllByCompany_Id(companyId).stream().map(mapper::toDomain).toList();
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
