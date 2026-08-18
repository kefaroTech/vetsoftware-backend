package com.vetsoftware.app.laboratorytesttype.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.laboratorytesttype.application.port.out.LaboratoryTestTypeRepository;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaLaboratoryTestTypeRepository implements LaboratoryTestTypeRepository {
    private final LaboratoryTestTypeJpaRepository jpaRepository;
    private final LaboratoryTestTypeJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaLaboratoryTestTypeRepository(LaboratoryTestTypeJpaRepository jpaRepository,
            LaboratoryTestTypeJpaMapper mapper, CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public LaboratoryTestType save(LaboratoryTestType laboratoryTestType) {
        CompanyJpaEntity company = laboratoryTestType.getCompany() == null
                ? null
                : companyJpaRepository.getReferenceById(laboratoryTestType.getCompany().id());
        LaboratoryTestTypeJpaEntity saved = jpaRepository
                .save(mapper.toJpa(laboratoryTestType, company));
        return mapper.toDomain(saved, laboratoryTestType.getCompany());
    }

    @Override
    public Optional<LaboratoryTestType> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<LaboratoryTestType> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findAvailableById(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<LaboratoryTestType> findOwnedByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<LaboratoryTestType> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<LaboratoryTestType> findAllAvailableForCompany(Long companyId) {
        return jpaRepository.findAllByGeneralTrueOrCompany_Id(companyId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public int reactivate(Long id, Long companyId) {
        return jpaRepository.reactivate(id, companyId);
    }
}
