package com.vetsoftware.app.legaldocumentversion.infrastructure.persistence;

import com.vetsoftware.app.legaldocumentversion.application.port.out.LegalDocumentVersionRepository;
import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentVersion;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaLegalDocumentVersionRepository implements LegalDocumentVersionRepository {

    private final LegalDocumentVersionJpaRepository jpaRepository;
    private final LegalDocumentVersionJpaMapper mapper;

    public JpaLegalDocumentVersionRepository(LegalDocumentVersionJpaRepository jpaRepository,
            LegalDocumentVersionJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public LegalDocumentVersion save(LegalDocumentVersion version) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(version)));
    }

    /**
     * {@code saveAndFlush} y no {@code save}: el UPDATE que cierra la vigencia
     * tiene que llegar a la base antes de que se inserte la version que la sucede,
     * o {@code uq_ldv_current} ve dos vigentes a la vez. Ver el contrato del
     * puerto.
     */
    @Override
    public LegalDocumentVersion supersede(LegalDocumentVersion version) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toJpa(version)));
    }

    @Override
    public Optional<LegalDocumentVersion> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<LegalDocumentVersion> findCurrentByCode(String code) {
        return jpaRepository.findByCodeAndSupersededAtIsNull(code).map(mapper::toDomain);
    }

    @Override
    public Optional<LegalDocumentVersion> findByCodeAndContentHash(String code,
            String contentHash) {
        return jpaRepository.findByCodeAndContentHash(code, contentHash).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCodeAndContentHash(String code, String contentHash) {
        return jpaRepository.existsByCodeAndContentHash(code, contentHash);
    }

    @Override
    public Optional<Integer> findLastDocumentVersion(String code) {
        return jpaRepository.findLastDocumentVersion(code);
    }

    @Override
    public PageResult<LegalDocumentVersion> findAllByCode(String code, int page, int pageSize) {
        Sort orden = Sort.by(Sort.Direction.DESC, "documentVersion")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        return Pages.result(jpaRepository.findByCode(code, Pages.request(page, pageSize, orden)),
                mapper::toDomain);
    }
}
