package com.vetsoftware.app.configurator.infrastructure.persistence;

import com.vetsoftware.app.configurator.domain.ConfiguratorQuestion;
import org.springframework.stereotype.Component;

@Component
public class ConfiguratorQuestionJpaMapper {

    public ConfiguratorQuestionJpaEntity toJpa(ConfiguratorQuestion question) {
        ConfiguratorQuestionJpaEntity entity = new ConfiguratorQuestionJpaEntity();
        entity.setId(question.getId());
        entity.setCode(question.getCode());
        entity.setQuestionText(question.getQuestionText());
        entity.setHelpText(question.getHelpText());
        entity.setAnswerType(question.getAnswerType());
        entity.setParentOptionId(question.getParentOptionId());
        entity.setRequired(question.isRequired());
        entity.setSortOrder(question.getSortOrder());
        entity.setCreatedDate(question.getCreatedDate());
        entity.setVersion(question.getVersion());
        entity.setEnabled(question.isEnabled());
        return entity;
    }

    public ConfiguratorQuestion toDomain(ConfiguratorQuestionJpaEntity entity) {
        return new ConfiguratorQuestion(entity.getId(), entity.getCode(), entity.getQuestionText(),
                entity.getHelpText(), entity.getAnswerType(), entity.getParentOptionId(),
                entity.isRequired(), entity.getSortOrder(), entity.getCreatedDate(),
                entity.getVersion(), entity.isEnabled());
    }
}
