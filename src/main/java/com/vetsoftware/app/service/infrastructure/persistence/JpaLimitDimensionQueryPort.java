package com.vetsoftware.app.service.infrastructure.persistence;

import com.vetsoftware.app.limitdimension.infrastructure.persistence.LimitDimensionJpaRepository;
import com.vetsoftware.app.service.application.port.out.LimitDimensionQueryPort;
import com.vetsoftware.app.service.domain.LimitDimensionRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * El único archivo de esta feature que conoce {@code limitdimension}, por la
 * excepción acotada del {@code CLAUDE.md}.
 *
 * <p>
 * Lleva nombre de bean explícito porque ya hay más de una clase con este mismo
 * nombre simple, una por rodaja que referencia un eje ({@code catalogitem},
 * {@code companyusageevent}); un nombre por defecto colisionaría al arrancar el
 * contexto.
 */
@Component("serviceJpaLimitDimensionQueryPort")
public class JpaLimitDimensionQueryPort implements LimitDimensionQueryPort {

    private final LimitDimensionJpaRepository limitDimensionJpaRepository;

    public JpaLimitDimensionQueryPort(LimitDimensionJpaRepository limitDimensionJpaRepository) {
        this.limitDimensionJpaRepository = limitDimensionJpaRepository;
    }

    @Override
    public Optional<LimitDimensionRef> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return limitDimensionJpaRepository.findByCode(code)
                .map(entity -> new LimitDimensionRef(entity.getId(), entity.getCode()));
    }
}
