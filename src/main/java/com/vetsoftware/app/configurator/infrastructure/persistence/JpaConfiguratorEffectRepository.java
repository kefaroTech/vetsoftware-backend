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

    /**
     * El mismo orden que aplica el resolvedor: {@code priority} ascendente y, a
     * igualdad, {@code id}. La prioridad <strong>no</strong> es única —dos efectos
     * de la misma pregunta comparten decena a propósito—, así que sin el desempate
     * por la clave primaria el orden no sería total y dos páginas consecutivas
     * repetirían u omitirían efectos.
     */
    private static final Sort ORDEN = Sort.by(Sort.Direction.ASC, "priority")
            .and(Sort.by(Sort.Direction.ASC, "id"));

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
        return jpaRepository.findAllByOrderByPriorityAscIdAsc().stream().map(mapper::toDomain)
                .toList();
    }

    /**
     * {@code findAllById} de Spring Data respeta el {@code @SQLRestriction} de la
     * entidad, así que un efecto dado de baja no vuelve — que es justo lo que hace
     * falta: reordenar un efecto inactivo no significa nada.
     */
    @Override
    public List<ConfiguratorEffect> findAllByIds(List<Long> ids) {
        return jpaRepository.findAllById(ids).stream().map(mapper::toDomain).toList();
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
