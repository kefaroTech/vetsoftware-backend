package com.vetsoftware.app.aiproposal.infrastructure.retention;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registra las propiedades de retencion, como {@code TokenCleanupConfig}. */
@Configuration
@EnableConfigurationProperties(AiProposalRetentionProperties.class)
public class AiProposalRetentionConfig {
}
