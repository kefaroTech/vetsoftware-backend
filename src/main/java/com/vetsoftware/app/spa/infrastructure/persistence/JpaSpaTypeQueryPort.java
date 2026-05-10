package com.vetsoftware.app.spa.infrastructure.persistence;

import com.vetsoftware.app.spa.application.port.out.SpaTypeQueryPort;
import com.vetsoftware.app.spa.domain.SpaTypeRef;
import com.vetsoftware.app.spatype.infrastructure.persistence.SpaTypeJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("spaJpaSpaTypeQueryPort")
public class JpaSpaTypeQueryPort implements SpaTypeQueryPort {
    private final SpaTypeJpaRepository spaTypeJpaRepository;

    public JpaSpaTypeQueryPort(SpaTypeJpaRepository spaTypeJpaRepository) {
        this.spaTypeJpaRepository = spaTypeJpaRepository;
    }

    @Override
    public Optional<SpaTypeRef> findById(Long spaTypeId) {
        return spaTypeJpaRepository.findById(spaTypeId)
            .map(e -> new SpaTypeRef(e.getId(), e.getName()));
    }
}
