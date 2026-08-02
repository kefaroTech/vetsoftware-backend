package com.vetsoftware.app.systemuser.infrastructure.persistence;

import com.vetsoftware.app.systemuser.application.port.out.SystemUserRepository;
import com.vetsoftware.app.systemuser.domain.SystemUser;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSystemUserRepository implements SystemUserRepository {
    private final SystemUserJpaRepository jpaRepository;
    private final SystemUserJpaMapper mapper;

    public JpaSystemUserRepository(SystemUserJpaRepository jpaRepository,
            SystemUserJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public SystemUser save(SystemUser systemUser) {
        SystemUserJpaEntity saved = jpaRepository.save(mapper.toJpa(systemUser));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<SystemUser> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<SystemUser> findAll() {
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
