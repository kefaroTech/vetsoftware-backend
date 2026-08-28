package com.vetsoftware.app.uvtvalue.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.uvtvalue.domain.UvtValue;
import java.util.Optional;

public interface UvtValueRepository {

    UvtValue save(UvtValue value);

    Optional<UvtValue> findById(Long id);

    /** La unica lectura de negocio: por ano. No existe «la vigente». */
    Optional<UvtValue> findByFiscalYear(int fiscalYear);

    boolean existsByFiscalYear(int fiscalYear);

    PageResult<UvtValue> findAll(int page, int pageSize);
}
