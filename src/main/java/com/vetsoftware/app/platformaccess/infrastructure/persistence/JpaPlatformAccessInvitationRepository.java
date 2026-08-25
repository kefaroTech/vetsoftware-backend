package com.vetsoftware.app.platformaccess.infrastructure.persistence;

import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessInvitationRepository;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessInvitation;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPlatformAccessInvitationRepository implements PlatformAccessInvitationRepository {

    private final PlatformAccessInvitationJpaRepository jpaRepository;
    private final PlatformAccessInvitationJpaMapper mapper;

    public JpaPlatformAccessInvitationRepository(
            PlatformAccessInvitationJpaRepository jpaRepository,
            PlatformAccessInvitationJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public PlatformAccessInvitation save(PlatformAccessInvitation invitation) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(invitation)));
    }

    @Override
    public Optional<PlatformAccessInvitation> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public int consume(Long id, Long systemUserId, LocalDateTime now) {
        return jpaRepository.consume(id, systemUserId, now);
    }
}
