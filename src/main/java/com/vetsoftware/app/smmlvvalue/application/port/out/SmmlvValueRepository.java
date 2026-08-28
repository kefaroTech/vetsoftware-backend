package com.vetsoftware.app.smmlvvalue.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.smmlvvalue.domain.SmmlvValue;
import java.util.Optional;

public interface SmmlvValueRepository {

    SmmlvValue save(SmmlvValue value);

    Optional<SmmlvValue> findById(Long id);

    Optional<SmmlvValue> findByFiscalYear(int fiscalYear);

    boolean existsByFiscalYear(int fiscalYear);

    PageResult<SmmlvValue> findAll(int page, int pageSize);
}
