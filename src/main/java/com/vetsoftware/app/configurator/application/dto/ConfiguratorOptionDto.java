package com.vetsoftware.app.configurator.application.dto;

import com.vetsoftware.app.configurator.domain.ConfiguratorOption;
import java.time.LocalDateTime;

public record ConfiguratorOptionDto(Long id, Long questionId, String code, String label,
        String helpText, int sortOrder, LocalDateTime createdDate, boolean enabled) {

    public static ConfiguratorOptionDto from(ConfiguratorOption option) {
        return new ConfiguratorOptionDto(option.getId(), option.getQuestionId(), option.getCode(),
                option.getLabel(), option.getHelpText(), option.getSortOrder(),
                option.getCreatedDate(), option.isEnabled());
    }
}
