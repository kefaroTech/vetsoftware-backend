package com.vetsoftware.app.smmlvvalue.infrastructure.persistence;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.smmlvvalue.application.port.out.SmmlvValueRepository;
import com.vetsoftware.app.smmlvvalue.domain.SmmlvValue;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSmmlvValueRepository implements SmmlvValueRepository {

    private final SmmlvValueJpaRepository jpaRepository;
    private final SmmlvValueJpaMapper mapper;

    public JpaSmmlvValueRepository(SmmlvValueJpaRepository jpaRepository,
            SmmlvValueJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public SmmlvValue save(SmmlvValue value) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(value)));
    }

    @Override
    public Optional<SmmlvValue> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<SmmlvValue> findByFiscalYear(int fiscalYear) {
        return jpaRepository.findByFiscalYear((short) fiscalYear).map(mapper::toDomain);
    }

    @Override
    public boolean existsByFiscalYear(int fiscalYear) {
        return jpaRepository.existsByFiscalYear((short) fiscalYear);
    }

    @Override
    public PageResult<SmmlvValue> findAll(int page, int pageSize) {
        Sort orden = Sort.by(Sort.Direction.DESC, "fiscalYear")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, orden)),
                mapper::toDomain);
    }
}
