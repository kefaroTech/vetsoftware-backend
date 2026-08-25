package com.vetsoftware.app.platformaccess.infrastructure.persistence;

import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessRequestRepository;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessDecision;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessRequest;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPlatformAccessRequestRepository implements PlatformAccessRequestRepository {

    private final PlatformAccessRequestJpaRepository jpaRepository;
    private final PlatformAccessRequestJpaMapper mapper;

    public JpaPlatformAccessRequestRepository(PlatformAccessRequestJpaRepository jpaRepository,
            PlatformAccessRequestJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public PlatformAccessRequest save(PlatformAccessRequest request) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(request)));
    }

    @Override
    public Optional<PlatformAccessRequest> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<PlatformAccessRequest> findByApprovalTokenHash(String approvalTokenHash) {
        return jpaRepository.findByApprovalTokenHash(approvalTokenHash).map(mapper::toDomain);
    }

    @Override
    public Optional<PlatformAccessRequest> findLivePendingByEmail(String email, LocalDateTime now) {
        // Limit(1) y no un findFirst derivado: la consulta es explicita y el tope
        // viaja con ella. Como mucho deberia haber una viva; si hubiera mas de una
        // por una carrera, gana la mas reciente y las otras caducan solas.
        return jpaRepository.findLivePendingByEmail(email, now, Limit.of(1)).stream().findFirst()
                .map(mapper::toDomain);
    }

    @Override
    public int registerFailedAttempt(Long id) {
        return jpaRepository.registerFailedAttempt(id);
    }

    @Override
    public int applyDecision(Long id, PlatformAccessDecision decision, LocalDateTime now) {
        return jpaRepository.applyDecision(id, decision.name(), now);
    }
}
