package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import com.vetsoftware.app.catalogitem.application.port.out.LimitDimensionQueryPort;
import com.vetsoftware.app.catalogitem.domain.LimitDimensionRef;
import com.vetsoftware.app.limitdimension.infrastructure.persistence.LimitDimensionJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de esta feature que conoce {@code limitdimension}, por la
 * excepcion acotada del {@code CLAUDE.md}.
 *
 * <p>
 * <strong>Lleva nombre de bean explicito.</strong> Hay tres clases con este
 * mismo nombre simple —una por rodaja que referencia un eje— y el nombre por
 * defecto que Spring deriva de la clase es el nombre simple: dos beans
 * homonimos no arrancan el contexto. Mismo motivo por el que
 * {@code JpaPlatformCatalogPort} lo lleva.
 */
@Component("catalogItemJpaLimitDimensionQueryPort")
public class JpaLimitDimensionQueryPort implements LimitDimensionQueryPort {

    private final LimitDimensionJpaRepository limitDimensionJpaRepository;

    public JpaLimitDimensionQueryPort(LimitDimensionJpaRepository limitDimensionJpaRepository) {
        this.limitDimensionJpaRepository = limitDimensionJpaRepository;
    }

    @Override
    public Optional<LimitDimensionRef> findByCode(String code) {
        if (code == null || code.isBlank())
            return Optional.empty();
        return limitDimensionJpaRepository.findByCode(code)
                .map(entity -> new LimitDimensionRef(entity.getId(), entity.getCode()));
    }
}
