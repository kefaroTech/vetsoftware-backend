package com.vetsoftware.app.spatype.infrastructure.persistence;

import com.vetsoftware.app.spatype.application.port.out.SpaTypeRepository;
import com.vetsoftware.app.spatype.domain.SpaType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSpaTypeRepository implements SpaTypeRepository {
    private final SpaTypeJpaRepository jpaRepository;
    private final SpaTypeJpaMapper mapper;

    public JpaSpaTypeRepository(SpaTypeJpaRepository jpaRepository, SpaTypeJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public SpaType save(SpaType spaType) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(spaType)));
    }

    @Override
    public Optional<SpaType> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<SpaType> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
