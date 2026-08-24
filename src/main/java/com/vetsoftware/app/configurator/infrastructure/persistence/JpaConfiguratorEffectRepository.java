package com.vetsoftware.app.configurator.infrastructure.persistence;

import com.vetsoftware.app.configurator.application.dto.LinkStateDto;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorEffectRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffect;
import com.vetsoftware.app.configurator.domain.EffectType;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaConfiguratorEffectRepository implements ConfiguratorEffectRepository {

    /** El id ya es total por sí solo: es la clave primaria. */
    private static final Sort ORDEN = Sort.by(Sort.Direction.ASC, "id");

    private final ConfiguratorEffectJpaRepository jpaRepository;
    private final ConfiguratorEffectJpaMapper mapper;

    public JpaConfiguratorEffectRepository(ConfiguratorEffectJpaRepository jpaRepository,
            ConfiguratorEffectJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ConfiguratorEffect save(ConfiguratorEffect effect) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(effect)));
    }

    @Override
    public Optional<ConfiguratorEffect> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ConfiguratorEffect> findAllOrdered() {
        return jpaRepository.findAllByOrderByIdAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<ConfiguratorEffect> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, ORDEN)),
                mapper::toDomain);
    }

    @Override
    public boolean existsByOptionId(Long optionId) {
        return jpaRepository.existsByOptionId(optionId);
    }

    @Override
    public boolean existsByQuestionId(Long questionId) {
        return jpaRepository.existsByQuestionId(questionId);
    }

    @Override
    public boolean existsQuantityFromAnswerByQuestionId(Long questionId) {
        return jpaRepository.existsByQuestionIdAndEffect(questionId,
                EffectType.QUANTITY_FROM_ANSWER);
    }

    /**
     * Despacha a la clave única que corresponde al disparador. La entidad garantiza
     * que viene exactamente uno de los dos, y MySQL admite múltiples {@code NULL}
     * en un índice único: por eso hay dos consultas y no una con {@code OR}, que
     * además obligaría a bindear un parámetro nulo tipado.
     */
    @Override
    public Optional<LinkStateDto> findAnyByTrigger(Long optionId, Long questionId,
            Long catalogItemId, EffectType effect) {
        String effectName = effect.name();
        if (optionId != null) {
            return jpaRepository.findAnyIdByOptionTrigger(optionId, catalogItemId, effectName)
                    .map(id -> new LinkStateDto(id, jpaRepository
                            .countEnabledByOptionTrigger(optionId, catalogItemId, effectName) > 0));
        }
        return jpaRepository.findAnyIdByQuestionTrigger(questionId, catalogItemId, effectName)
                .map(id -> new LinkStateDto(id, jpaRepository
                        .countEnabledByQuestionTrigger(questionId, catalogItemId, effectName) > 0));
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
