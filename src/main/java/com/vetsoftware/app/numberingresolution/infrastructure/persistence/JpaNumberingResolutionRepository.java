package com.vetsoftware.app.numberingresolution.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.numberingresolution.application.port.out.NumberingResolutionRepository;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolution;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaNumberingResolutionRepository implements NumberingResolutionRepository {
    private final NumberingResolutionJpaRepository jpaRepository;
    private final NumberingResolutionJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaNumberingResolutionRepository(NumberingResolutionJpaRepository jpaRepository,
                                            NumberingResolutionJpaMapper mapper,
                                            CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public NumberingResolution save(NumberingResolution resolution) {
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(resolution.getCompany().id());
        NumberingResolutionJpaEntity saved = jpaRepository.save(mapper.toJpa(resolution, company));
        return mapper.toDomain(saved, resolution.getCompany());
    }

    @Override
    public Optional<NumberingResolution> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<NumberingResolution> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<NumberingResolution> findAllByCompanyId(Long companyId) {
        return jpaRepository.findAllByCompanyId(companyId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsActiveByCompanyBranchAndType(Long companyId, Long branchId,
            com.vetsoftware.app.numberingresolution.domain.ElectronicDocumentType documentType) {
        return jpaRepository.countActiveByCompanyBranchAndType(companyId, branchId, documentType.name()) > 0;
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
