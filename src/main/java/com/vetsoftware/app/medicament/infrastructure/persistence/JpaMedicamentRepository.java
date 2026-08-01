package com.vetsoftware.app.medicament.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.Medicament;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaMedicamentRepository implements MedicamentRepository {
    private final MedicamentJpaRepository jpaRepository;
    private final MedicamentJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaMedicamentRepository(MedicamentJpaRepository jpaRepository,
                                   MedicamentJpaMapper mapper,
                                   CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Medicament save(Medicament medicament) {
        CompanyJpaEntity company = medicament.getCompany() == null ? null
                : companyJpaRepository.getReferenceById(medicament.getCompany().id());
        MedicamentJpaEntity saved = jpaRepository.save(mapper.toJpa(medicament, company));
        return mapper.toDomain(saved, medicament.getCompany());
    }

    @Override
    public Optional<Medicament> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Medicament> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findAvailableById(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<Medicament> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Medicament> findAllAvailableForCompany(Long companyId) {
        return jpaRepository.findAllByGeneralTrueOrCompany_Id(companyId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Medicament> findAllDisabledForCompany(Long companyId) {
        return jpaRepository.findAllDisabledForCompany(companyId)
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
