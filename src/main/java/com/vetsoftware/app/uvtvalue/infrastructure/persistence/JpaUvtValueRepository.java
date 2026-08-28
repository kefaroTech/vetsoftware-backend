package com.vetsoftware.app.uvtvalue.infrastructure.persistence;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.uvtvalue.application.port.out.UvtValueRepository;
import com.vetsoftware.app.uvtvalue.domain.UvtValue;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaUvtValueRepository implements UvtValueRepository {

    private final UvtValueJpaRepository jpaRepository;
    private final UvtValueJpaMapper mapper;

    public JpaUvtValueRepository(UvtValueJpaRepository jpaRepository, UvtValueJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public UvtValue save(UvtValue value) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(value)));
    }

    @Override
    public Optional<UvtValue> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<UvtValue> findByFiscalYear(int fiscalYear) {
        return jpaRepository.findByFiscalYear((short) fiscalYear).map(mapper::toDomain);
    }

    @Override
    public boolean existsByFiscalYear(int fiscalYear) {
        return jpaRepository.existsByFiscalYear((short) fiscalYear);
    }

    @Override
    public PageResult<UvtValue> findAll(int page, int pageSize) {
        Sort orden = Sort.by(Sort.Direction.DESC, "fiscalYear")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, orden)),
                mapper::toDomain);
    }
}
