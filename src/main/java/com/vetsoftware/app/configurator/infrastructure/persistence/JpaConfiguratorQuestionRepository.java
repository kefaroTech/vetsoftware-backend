package com.vetsoftware.app.configurator.infrastructure.persistence;

import com.vetsoftware.app.configurator.application.dto.LinkStateDto;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorQuestionRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorQuestion;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaConfiguratorQuestionRepository implements ConfiguratorQuestionRepository {

    /**
     * El mismo orden del cuestionario, y total: {@code sort_order} se repite entre
     * preguntas, así que sin el desempate por {@code id} dos páginas consecutivas
     * pueden repetir u omitir filas.
     */
    private static final Sort ORDEN = Sort.by(Sort.Direction.ASC, "sortOrder")
            .and(Sort.by(Sort.Direction.ASC, "id"));

    private final ConfiguratorQuestionJpaRepository jpaRepository;
    private final ConfiguratorQuestionJpaMapper mapper;

    public JpaConfiguratorQuestionRepository(ConfiguratorQuestionJpaRepository jpaRepository,
            ConfiguratorQuestionJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ConfiguratorQuestion save(ConfiguratorQuestion question) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(question)));
    }

    @Override
    public Optional<ConfiguratorQuestion> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ConfiguratorQuestion> findAllOrdered() {
        return jpaRepository.findAllByOrderBySortOrderAscIdAsc().stream().map(mapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<ConfiguratorQuestion> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, ORDEN)),
                mapper::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }

    @Override
    public boolean existsByParentOptionId(Long optionId) {
        return jpaRepository.existsByParentOptionId(optionId);
    }

    @Override
    public Optional<LinkStateDto> findAnyByCode(String code) {
        return jpaRepository.findAnyIdByCode(code)
                .map(id -> new LinkStateDto(id, jpaRepository.countEnabledByCode(code) > 0));
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
