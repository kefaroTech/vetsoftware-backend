package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaElectronicDocumentRepository implements ElectronicDocumentRepository {
    private final ElectronicDocumentJpaRepository jpaRepository;
    private final ElectronicDocumentJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaElectronicDocumentRepository(ElectronicDocumentJpaRepository jpaRepository,
                                           ElectronicDocumentJpaMapper mapper,
                                           CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public ElectronicDocument save(ElectronicDocument document) {
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(document.getCompanyId());
        ElectronicDocumentJpaEntity saved = jpaRepository.save(mapper.toJpa(document, company));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ElectronicDocument> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ElectronicDocument> findAllByCompanyId(Long companyId) {
        return jpaRepository.findByCompanyId(companyId).stream()
                .distinct()
                .map(mapper::toDomain)
                .toList();
    }
}
