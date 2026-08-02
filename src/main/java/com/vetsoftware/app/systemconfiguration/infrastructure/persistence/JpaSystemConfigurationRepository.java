package com.vetsoftware.app.systemconfiguration.infrastructure.persistence;

import com.vetsoftware.app.systemconfiguration.application.port.out.SystemConfigurationRepository;
import com.vetsoftware.app.systemconfiguration.domain.SystemConfiguration;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSystemConfigurationRepository implements SystemConfigurationRepository {
    private final SystemConfigurationJpaRepository jpaRepository;
    private final SystemConfigurationJpaMapper mapper;

    public JpaSystemConfigurationRepository(SystemConfigurationJpaRepository jpaRepository,
            SystemConfigurationJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public SystemConfiguration save(SystemConfiguration config) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(config)));
    }

    @Override
    public Optional<SystemConfiguration> findByPropertyName(String propertyName) {
        return jpaRepository.findByPropertyName(propertyName).map(mapper::toDomain);
    }

    @Override
    public List<SystemConfiguration> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }
}
