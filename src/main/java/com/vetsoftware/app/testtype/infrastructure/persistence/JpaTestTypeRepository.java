package com.vetsoftware.app.testtype.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.testtype.application.port.out.TestTypeRepository;
import com.vetsoftware.app.testtype.domain.TestType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTestTypeRepository implements TestTypeRepository {
    private final TestTypeJpaRepository jpaRepository;
    private final TestTypeJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaTestTypeRepository(TestTypeJpaRepository jpaRepository,
                                 TestTypeJpaMapper mapper,
                                 CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public TestType save(TestType testType) {
        CompanyJpaEntity company = testType.getCompany() == null ? null
                : companyJpaRepository.getReferenceById(testType.getCompany().id());
        TestTypeJpaEntity saved = jpaRepository.save(mapper.toJpa(testType, company));
        return mapper.toDomain(saved, testType.getCompany());
    }

    @Override
    public Optional<TestType> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<TestType> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
