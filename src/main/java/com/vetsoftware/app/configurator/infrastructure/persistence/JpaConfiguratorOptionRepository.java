package com.vetsoftware.app.configurator.infrastructure.persistence;

import com.vetsoftware.app.configurator.application.dto.LinkStateDto;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorOptionRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class JpaConfiguratorOptionRepository implements ConfiguratorOptionRepository {

    private final ConfiguratorOptionJpaRepository jpaRepository;
    private final ConfiguratorOptionJpaMapper mapper;

    public JpaConfiguratorOptionRepository(ConfiguratorOptionJpaRepository jpaRepository,
            ConfiguratorOptionJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ConfiguratorOption save(ConfiguratorOption option) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(option)));
    }

    @Override
    public Optional<ConfiguratorOption> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ConfiguratorOption> findAllOrdered() {
        return jpaRepository.findAllByOrderByQuestionIdAscSortOrderAscIdAsc().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<ConfiguratorOption> findByQuestionId(Long questionId) {
        return jpaRepository.findByQuestionIdOrderBySortOrderAscIdAsc(questionId).stream()
                .map(mapper::toDomain).toList();
    }

    /**
     * Una consulta y un agrupado en memoria, no una consulta por pregunta.
     * {@code LinkedHashMap} para conservar el orden que trae el {@code ORDER BY}:
     * el editor pinta las opciones en ese orden y un {@code HashMap} lo perderia
     * entre ejecuciones.
     */
    @Override
    public Map<Long, List<ConfiguratorOption>> findByQuestionIds(Collection<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty())
            return Map.of();
        List<Long> ids = questionIds.stream().filter(java.util.Objects::nonNull).distinct()
                .toList();
        if (ids.isEmpty())
            return Map.of();
        return jpaRepository.findByQuestionIdInOrderByQuestionIdAscSortOrderAscIdAsc(ids).stream()
                .map(mapper::toDomain)
                .collect(Collectors.groupingBy(ConfiguratorOption::getQuestionId,
                        LinkedHashMap::new, Collectors.toList()));
    }

    @Override
    public boolean existsByQuestionIdAndCode(Long questionId, String code) {
        return jpaRepository.existsByQuestionIdAndCode(questionId, code);
    }

    @Override
    public boolean existsByQuestionId(Long questionId) {
        return jpaRepository.existsByQuestionId(questionId);
    }

    @Override
    public Optional<LinkStateDto> findAnyByQuestionIdAndCode(Long questionId, String code) {
        return jpaRepository.findAnyIdByQuestionIdAndCode(questionId, code)
                .map(id -> new LinkStateDto(id,
                        jpaRepository.countEnabledByQuestionIdAndCode(questionId, code) > 0));
    }

    @Override
    public int reactivate(Long id) {
        return jpaRepository.reactivate(id);
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
