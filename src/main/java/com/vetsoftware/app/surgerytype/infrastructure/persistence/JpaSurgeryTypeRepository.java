package com.vetsoftware.app.surgerytype.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.surgerytype.application.port.out.SurgeryTypeRepository;
import com.vetsoftware.app.surgerytype.domain.SurgeryType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSurgeryTypeRepository implements SurgeryTypeRepository {
    private final SurgeryTypeJpaRepository jpaRepository;
    private final SurgeryTypeJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaSurgeryTypeRepository(SurgeryTypeJpaRepository jpaRepository,
            SurgeryTypeJpaMapper mapper, CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public SurgeryType save(SurgeryType surgeryType) {
        CompanyJpaEntity company = surgeryType.getCompany() == null
                ? null
                : companyJpaRepository.getReferenceById(surgeryType.getCompany().id());
        SurgeryTypeJpaEntity saved = jpaRepository.save(mapper.toJpa(surgeryType, company));
        return mapper.toDomain(saved, surgeryType.getCompany());
    }

    @Override
    public Optional<SurgeryType> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<SurgeryType> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findAvailableById(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<SurgeryType> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<SurgeryType> findAllAvailableForCompany(Long companyId) {
        return jpaRepository.findAllByGeneralTrueOrCompany_Id(companyId).stream()
                .map(mapper::toDomain).toList();
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
