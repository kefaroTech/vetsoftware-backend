package com.vetsoftware.app.testtype.infrastructure.persistence;

import com.vetsoftware.app.testtype.application.port.out.TestTypeRepository;
import com.vetsoftware.app.testtype.domain.TestType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTestTypeRepository implements TestTypeRepository {
    private final TestTypeJpaRepository jpaRepository;
    private final TestTypeJpaMapper mapper;

    public JpaTestTypeRepository(TestTypeJpaRepository jpaRepository, TestTypeJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public TestType save(TestType testType) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(testType)));
    }

    @Override
    public Optional<TestType> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<TestType> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
