package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.limitdimension.infrastructure.persistence.LimitDimensionJpaRepository;
import com.vetsoftware.app.subscription.application.port.out.LimitDimensionQueryPort;
import com.vetsoftware.app.subscription.domain.LimitDimensionRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de esta feature que conoce {@code limitdimension}, por la
 * excepcion acotada del {@code CLAUDE.md}.
 *
 * <p>
 * <strong>Lleva nombre de bean explicito</strong>, igual que
 * {@code subscriptionJpaPlatformCatalogPort}: hay tres clases con este nombre
 * simple —una por rodaja que referencia un eje— y Spring deriva el nombre del
 * bean del nombre simple de la clase, asi que dos homonimas no arrancan el
 * contexto.
 */
@Component("subscriptionJpaLimitDimensionQueryPort")
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
