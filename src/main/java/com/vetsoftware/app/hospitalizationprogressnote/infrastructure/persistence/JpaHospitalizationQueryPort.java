package com.vetsoftware.app.hospitalizationprogressnote.infrastructure.persistence;

import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaRepository;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.out.HospitalizationQueryPort;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("hospitalizationProgressNoteJpaHospitalizationQueryPort")
public class JpaHospitalizationQueryPort implements HospitalizationQueryPort {
    private final HospitalizationJpaRepository hospitalizationJpaRepository;

    public JpaHospitalizationQueryPort(HospitalizationJpaRepository hospitalizationJpaRepository) {
        this.hospitalizationJpaRepository = hospitalizationJpaRepository;
    }

    @Override
    public Optional<HospitalizationRef> findById(Long hospitalizationId) {
        return hospitalizationJpaRepository.findById(hospitalizationId)
                .map(e -> new HospitalizationRef(e.getId(), e.getDate()));
    }
}
