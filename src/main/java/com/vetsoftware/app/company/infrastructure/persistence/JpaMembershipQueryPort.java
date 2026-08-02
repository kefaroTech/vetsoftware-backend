package com.vetsoftware.app.company.infrastructure.persistence;

import com.vetsoftware.app.company.application.port.out.MembershipQueryPort;
import com.vetsoftware.app.company.domain.MembershipRef;
import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaMembershipQueryPort implements MembershipQueryPort {
  private final MembershipJpaRepository membershipJpaRepository;

  public JpaMembershipQueryPort(MembershipJpaRepository membershipJpaRepository) {
    this.membershipJpaRepository = membershipJpaRepository;
  }

  @Override
  public Optional<MembershipRef> findById(Long membershipId) {
    return membershipJpaRepository
        .findById(membershipId)
        .map(e -> new MembershipRef(e.getId(), e.getName(), e.getStatus()));
  }
}
