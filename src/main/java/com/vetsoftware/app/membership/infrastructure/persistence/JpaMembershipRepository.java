package com.vetsoftware.app.membership.infrastructure.persistence;

import com.vetsoftware.app.membership.application.port.out.MembershipRepository;
import com.vetsoftware.app.membership.domain.Membership;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaMembershipRepository implements MembershipRepository {
  private final MembershipJpaRepository jpaRepository;
  private final MembershipJpaMapper mapper;

  public JpaMembershipRepository(
      MembershipJpaRepository jpaRepository, MembershipJpaMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public Membership save(Membership membership) {
    return mapper.toDomain(jpaRepository.save(mapper.toJpa(membership)));
  }

  @Override
  public Optional<Membership> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<Membership> findAll() {
    return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
  }

  @Override
  public void delete(Long id) {
    jpaRepository.deleteById(id);
  }

  @Override
  public int reactivate(Long id) {
    return jpaRepository.reactivate(id);
  }
}
