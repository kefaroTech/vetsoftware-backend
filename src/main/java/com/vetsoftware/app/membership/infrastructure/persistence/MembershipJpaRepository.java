package com.vetsoftware.app.membership.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipJpaRepository extends JpaRepository<MembershipJpaEntity, Long> {}
