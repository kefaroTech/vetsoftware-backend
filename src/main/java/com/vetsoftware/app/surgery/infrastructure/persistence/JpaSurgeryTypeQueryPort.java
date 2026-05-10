package com.vetsoftware.app.surgery.infrastructure.persistence;

import com.vetsoftware.app.surgery.application.port.out.SurgeryTypeQueryPort;
import com.vetsoftware.app.surgery.domain.SurgeryTypeRef;
import com.vetsoftware.app.surgerytype.infrastructure.persistence.SurgeryTypeJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("surgeryJpaSurgeryTypeQueryPort")
public class JpaSurgeryTypeQueryPort implements SurgeryTypeQueryPort {
    private final SurgeryTypeJpaRepository surgeryTypeJpaRepository;

    public JpaSurgeryTypeQueryPort(SurgeryTypeJpaRepository surgeryTypeJpaRepository) {
        this.surgeryTypeJpaRepository = surgeryTypeJpaRepository;
    }

    @Override
    public Optional<SurgeryTypeRef> findById(Long surgeryTypeId) {
        return surgeryTypeJpaRepository.findById(surgeryTypeId)
            .map(e -> new SurgeryTypeRef(e.getId(), e.getName()));
    }
}
