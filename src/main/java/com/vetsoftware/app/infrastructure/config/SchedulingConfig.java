package com.vetsoftware.app.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita @Scheduled en el proyecto (usado por el job de reintento de
 * contingencia DIAN).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
