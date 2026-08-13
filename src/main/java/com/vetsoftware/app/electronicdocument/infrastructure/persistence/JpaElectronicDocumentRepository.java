package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.dto.PageResult;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaElectronicDocumentRepository implements ElectronicDocumentRepository {
    private static final int LIST_DEFAULT_PAGE_SIZE = 20;
    private static final int LIST_MAX_PAGE_SIZE = 200;

    private final ElectronicDocumentJpaRepository jpaRepository;
    private final ElectronicDocumentJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaElectronicDocumentRepository(ElectronicDocumentJpaRepository jpaRepository,
            ElectronicDocumentJpaMapper mapper, CompanyJpaRepository companyJpaRepository) {
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
    public boolean existsByOpenAccountIdAndDocumentType(Long openAccountId,
            com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType documentType) {
        return jpaRepository.existsByOpenAccountIdAndDocumentType(openAccountId, documentType);
    }

    @Override
    public Optional<ElectronicDocument> findByCompanyIdAndClientRequestId(Long companyId,
            String clientRequestId) {
        return jpaRepository.findByCompany_IdAndClientRequestId(companyId, clientRequestId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ElectronicDocument> findByOpenAccountId(Long openAccountId, Long companyId) {
        return jpaRepository.findByOpenAccountIdAndCompany_Id(openAccountId, companyId)
                .map(mapper::toDomain);
    }

    @Override
    public ElectronicDocument updateDianResult(ElectronicDocument document) {
        ElectronicDocumentJpaEntity entity = jpaRepository.findById(document.getId()).orElseThrow(
                () -> new com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentNotFoundException(
                        document.getId()));
        // Solo columnas del ciclo de vida DIAN + numeración fiscal; líneas y pagos no
        // se tocan.
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
        // F5: el reverso contable de la factura (marcado al validar su nota credito)
        // tambien se
        // persiste aqui.
        entity.setReversed(document.isReversed());
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public PageResult<ElectronicDocument> findAllByCompanyId(Long companyId, Long branchId,
            ElectronicDocumentType documentType, DianStatus dianStatus, int page, int pageSize) {
        // Dos consultas a proposito: la primera pagina ids (sin colecciones, LIMIT
        // real)
        // y la segunda hidrata solo esa pagina. Ver
        // findIdsByCompanyIdAndOptionalBranch.
        Page<Long> ids = jpaRepository.findIdsByCompanyIdAndOptionalBranch(companyId, branchId,
                documentType, dianStatus, listPageRequest(page, pageSize));
        if (ids.isEmpty()) {
            return new PageResult<>(List.of(), ids.getNumber(), ids.getSize(),
                    ids.getTotalElements(), ids.getTotalPages());
        }
        Map<Long, ElectronicDocument> byId = jpaRepository.findAllByIdsWithDetails(ids.getContent())
                .stream().map(mapper::toDomain)
                .collect(Collectors.toMap(ElectronicDocument::getId, Function.identity()));
        // El IN no garantiza orden: se reordena segun la pagina, que si viene ordenada.
        List<ElectronicDocument> content = ids.getContent().stream().map(byId::get)
                .filter(Objects::nonNull).toList();
        return new PageResult<>(content, ids.getNumber(), ids.getSize(), ids.getTotalElements(),
                ids.getTotalPages());
    }

    /**
     * Normaliza lo que llega del cliente y acota el tamano; sin tope se vuelve a
     * pedir la tabla entera por query param. Orden por id descendente: la
     * facturacion se lee de lo mas reciente hacia atras, y el orden explicito hace
     * la paginacion determinista.
     */
    private static PageRequest listPageRequest(int page, int pageSize) {
        int safeSize = pageSize <= 0
                ? LIST_DEFAULT_PAGE_SIZE
                : Math.min(pageSize, LIST_MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "id"));
    }

    @Override
    public List<ElectronicDocument> findByDianStatus(
            com.vetsoftware.app.electronicdocument.domain.DianStatus status) {
        return jpaRepository.findByDianStatus(status).stream().distinct().map(mapper::toDomain)
                .toList();
    }
}
