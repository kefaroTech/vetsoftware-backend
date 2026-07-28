package com.vetsoftware.app.infrastructure.pdf;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PdfProperties.class)
public class PdfConfig {
}
