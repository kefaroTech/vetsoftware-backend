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
    public Optional<ElectronicDocument> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<ElectronicDocument> findByCufe(String cufe, Long companyId) {
        return jpaRepository.findByCufeAndCompany_Id(cufe, companyId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByOpenAccountId(Long openAccountId) {
        return jpaRepository.existsByOpenAccountId(openAccountId);
    }

    @Override
    public Optional<ElectronicDocument> findByOpenAccountId(Long openAccountId, Long companyId) {
        return jpaRepository.findByOpenAccountIdAndCompany_Id(openAccountId, companyId).map(mapper::toDomain);
    }

    @Override
    public ElectronicDocument updateDianResult(ElectronicDocument document) {
        ElectronicDocumentJpaEntity entity = jpaRepository.findById(document.getId())
                .orElseThrow(() -> new com.vetsoftware.app.electronicdocument.domain
                        .ElectronicDocumentNotFoundException(document.getId()));
        // Solo columnas del ciclo de vida DIAN + numeración fiscal; líneas y pagos no se tocan.
        entity.setPrefix(document.getPrefix());
        entity.setConsecutive(document.getConsecutive());
        entity.setResolutionNumber(document.getResolutionNumber());
        entity.setCufe(document.getCufe());
        entity.setCude(document.getCude());
        entity.setUuid(document.getUuid());
        entity.setQrData(document.getQrData());
        entity.setQrUrl(document.getQrUrl());
        entity.setXmlSigned(document.getXmlSigned());
        entity.setPdfRepresentation(document.getPdfRepresentation());
        entity.setDianStatus(document.getDianStatus());
        entity.setDianValidationDate(document.getDianValidationDate());
        // F5: el reverso contable de la factura (marcado al validar su nota credito) tambien se persiste aqui.
        entity.setReversed(document.isReversed());
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<ElectronicDocument> findAllByCompanyId(Long companyId) {
        return jpaRepository.findByCompanyId(companyId).stream()
                .distinct()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ElectronicDocument> findByDianStatus(
            com.vetsoftware.app.electronicdocument.domain.DianStatus status) {
        return jpaRepository.findByDianStatus(status).stream()
                .distinct()
                .map(mapper::toDomain)
                .toList();
    }
}
