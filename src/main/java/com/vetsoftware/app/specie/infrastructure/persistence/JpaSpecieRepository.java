package com.vetsoftware.app.specie.infrastructure.persistence;

import com.vetsoftware.app.specie.application.port.out.SpecieRepository;
import com.vetsoftware.app.specie.domain.Specie;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSpecieRepository implements SpecieRepository {
    private final SpecieJpaRepository jpaRepository;
    private final SpecieJpaMapper mapper;

    public JpaSpecieRepository(SpecieJpaRepository jpaRepository, SpecieJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Specie save(Specie specie) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(specie)));
    }

    @Override
    public Optional<Specie> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Specie> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
